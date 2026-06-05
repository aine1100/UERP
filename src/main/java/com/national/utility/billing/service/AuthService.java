package com.national.utility.billing.service;

import com.national.utility.billing.config.AppProperties;
import com.national.utility.billing.dto.request.*;
import com.national.utility.billing.dto.response.AuthResponse;
import com.national.utility.billing.dto.response.MessageResponse;
import com.national.utility.billing.dto.response.UserResponse;
import com.national.utility.billing.dto.response.VerifyAccountResponse;
import com.national.utility.billing.exception.BusinessException;
import com.national.utility.billing.exception.ResourceNotFoundException;
import com.national.utility.billing.exception.UnauthorizedException;
import com.national.utility.billing.model.Customer;
import com.national.utility.billing.model.RefreshToken;
import com.national.utility.billing.model.User;
import com.national.utility.billing.model.enums.CustomerStatus;
import com.national.utility.billing.model.enums.UserRole;
import com.national.utility.billing.model.enums.UserStatus;
import com.national.utility.billing.repository.CustomerRepository;
import com.national.utility.billing.repository.RefreshTokenRepository;
import com.national.utility.billing.repository.UserRepository;
import com.national.utility.billing.security.JwtTokenProvider;
import com.national.utility.billing.security.UserPrincipal;
import com.national.utility.billing.service.mapper.EntityMapper;
import com.national.utility.billing.util.OtpGenerator;
import com.national.utility.billing.util.PhoneUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailService emailService;
    private final LocationService locationService;
    private final AppProperties appProperties;

    @Transactional
    public AuthResponse login(LoginRequest request) {
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            if (user.getStatus() == UserStatus.INVITED) {
                if (Boolean.TRUE.equals(user.getOtpVerified())) {
                    throw new UnauthorizedException(
                            "Your OTP is verified but your password is not set yet. Use setup-password first, then sign in.");
                }
                throw new UnauthorizedException(
                        "Account not activated. Verify your OTP, set your password, then sign in.");
            }
        });

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return buildAuthResponse(user, true);
    }

    @Transactional
    public VerifyAccountResponse verifyAccount(VerifyAccountRequest request) {
        User user = findInvitedUser(request.getEmail());
        validateInviteOtp(user, request.getOtp());

        user.setOtpVerified(true);
        userRepository.save(user);

        return VerifyAccountResponse.builder()
                .email(user.getEmail())
                .verified(true)
                .message("Account verified. Set your password, then sign in.")
                .build();
    }

    @Transactional
    public void resendOtp(ResendOtpRequest request) {
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            if (user.getStatus() == UserStatus.INVITED) {
                issueInviteOtp(user);
            }
        });
    }

    @Transactional
    public MessageResponse setupPassword(SetupPasswordRequest request) {
        User user = findInvitedUser(request.getEmail());

        if (request.getOtp() != null && !request.getOtp().isBlank()) {
            validateInviteOtp(user, request.getOtp());
            user.setOtpVerified(true);
        } else if (!Boolean.TRUE.equals(user.getOtpVerified())) {
            throw new BusinessException("Verify your OTP first, or include the OTP when setting your password.");
        }

        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setStatus(UserStatus.ACTIVE);
        user.setInviteToken(null);
        user.setInviteTokenExpiry(null);
        user.setOtpVerified(false);
        userRepository.save(user);

        return MessageResponse.builder()
                .message("Password set successfully. Please sign in with your email and new password.")
                .build();
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (refreshToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(refreshToken);
            throw new UnauthorizedException("Refresh token has expired");
        }

        User user = refreshToken.getUser();
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Account is not active");
        }

        refreshTokenRepository.delete(refreshToken);
        return buildAuthResponse(user, false);
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            if (user.getStatus() != UserStatus.ACTIVE || user.getPassword() == null) {
                return;
            }
            issueResetOtp(user);
        });
    }

    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException("Account is not active");
        }

        validateResetOtp(user, request.getOtp());

        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);

        refreshTokenRepository.deleteByUser(user);

        return MessageResponse.builder()
                .message("Password reset successfully. Please sign in with your new password.")
                .build();
    }

    @Transactional
    public void logout(RefreshTokenRequest request) {
        refreshTokenRepository.findByToken(request.getRefreshToken())
                .ifPresent(refreshTokenRepository::delete);
    }

    @Transactional
    public UserResponse inviteUser(InviteUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email is already registered");
        }

        if (request.getRole() == UserRole.CUSTOMER) {
            validateCustomerInviteFields(request);
            if (customerRepository.existsByNationalId(request.getNationalId())) {
                throw new BusinessException("National ID is already registered");
            }
            if (customerRepository.existsByEmail(request.getEmail())) {
                throw new BusinessException("Customer email is already registered");
            }
        }

        User user = User.builder()
                .fullNames(request.getFullNames())
                .email(request.getEmail())
                .phoneNumber(PhoneUtils.normalizeRwandaPhone(request.getPhoneNumber()))
                .status(UserStatus.INVITED)
                .role(request.getRole())
                .otpVerified(false)
                .build();

        user = userRepository.save(user);

        if (request.getRole() == UserRole.CUSTOMER) {
            Customer customer = Customer.builder()
                    .fullNames(request.getFullNames())
                    .nationalId(request.getNationalId())
                    .email(request.getEmail())
                    .phoneNumber(PhoneUtils.normalizeRwandaPhone(request.getPhoneNumber()))
                    .address(locationService.resolveSelection(request.getLocation()))
                    .status(CustomerStatus.ACTIVE)
                    .user(user)
                    .build();
            customerRepository.save(customer);
        }

        issueInviteOtp(user);

        return EntityMapper.toUserResponse(user);
    }

    private void issueInviteOtp(User user) {
        String otp = OtpGenerator.generateSixDigitOtp();
        user.setInviteToken(otp);
        user.setInviteTokenExpiry(LocalDateTime.now()
                .plusMinutes(appProperties.getOtp().getExpirationMinutes()));
        user.setOtpVerified(false);
        userRepository.save(user);

        emailService.sendVerificationOtpEmail(
                user.getEmail(),
                user.getFullNames(),
                otp,
                appProperties.getOtp().getExpirationMinutes());
    }

    private void issueResetOtp(User user) {
        String otp = OtpGenerator.generateSixDigitOtp();
        user.setResetToken(otp);
        user.setResetTokenExpiry(LocalDateTime.now()
                .plusMinutes(appProperties.getOtp().getExpirationMinutes()));
        userRepository.save(user);

        emailService.sendPasswordResetOtpEmail(
                user.getEmail(),
                user.getFullNames(),
                otp,
                appProperties.getOtp().getExpirationMinutes());
    }

    private User findInvitedUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getStatus() != UserStatus.INVITED) {
            throw new BusinessException("Account is already active or not eligible for verification");
        }
        return user;
    }

    private void validateInviteOtp(User user, String otp) {
        if (user.getInviteToken() == null || !user.getInviteToken().equals(otp)) {
            throw new BusinessException("Invalid OTP");
        }
        if (user.getInviteTokenExpiry() == null || user.getInviteTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new BusinessException("OTP has expired. Request a new one using your email address.");
        }
    }

    private void validateResetOtp(User user, String otp) {
        if (user.getResetToken() == null || !user.getResetToken().equals(otp)) {
            throw new BusinessException("Invalid OTP");
        }
        if (user.getResetTokenExpiry() == null || user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new BusinessException("OTP has expired. Request a new password reset code.");
        }
    }

    private AuthResponse buildAuthResponse(User user, boolean revokeExisting) {
        if (revokeExisting) {
            refreshTokenRepository.deleteByUser(user);
        }

        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshTokenValue = jwtTokenProvider.generateRefreshTokenValue();

        RefreshToken refreshToken = RefreshToken.builder()
                .token(refreshTokenValue)
                .expiryDate(LocalDateTime.now().plusSeconds(
                        jwtTokenProvider.getRefreshTokenExpirationMs() / 1000))
                .user(user)
                .build();
        refreshTokenRepository.save(refreshToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenValue)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessTokenExpirationMs() / 1000)
                .userId(user.getId())
                .email(user.getEmail())
                .fullNames(user.getFullNames())
                .role(user.getRole())
                .status(user.getStatus())
                .build();
    }

    private void validateCustomerInviteFields(InviteUserRequest request) {
        if (request.getNationalId() == null || request.getNationalId().isBlank()) {
            throw new BusinessException("National ID is required when inviting a customer");
        }
        if (request.getLocation() == null) {
            throw new BusinessException("Location is required when inviting a customer");
        }
    }
}
