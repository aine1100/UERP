package com.national.utility.billing.service;

import com.national.utility.billing.config.AppProperties;
import com.national.utility.billing.dto.request.*;
import com.national.utility.billing.dto.response.AuthResponse;
import com.national.utility.billing.dto.response.UserResponse;
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
import com.national.utility.billing.util.PhoneUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

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
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

        if (principal.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException(
                    "Account is not active. Please complete password setup using your invitation link.");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return buildAuthResponse(user, true);
    }

    @Transactional
    public AuthResponse setupPassword(SetupPasswordRequest request) {
        User user = userRepository.findByInviteToken(request.getInviteToken())
                .orElseThrow(() -> new ResourceNotFoundException("Invalid or expired invitation token"));

        if (user.getInviteTokenExpiry() == null || user.getInviteTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Invitation token has expired");
        }

        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setStatus(UserStatus.ACTIVE);
        user.setInviteToken(null);
        user.setInviteTokenExpiry(null);
        userRepository.save(user);

        return buildAuthResponse(user, true);
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

            String resetToken = UUID.randomUUID().toString();
            LocalDateTime expiry = LocalDateTime.now()
                    .plusHours(appProperties.getReset().getTokenExpirationHours());

            user.setResetToken(resetToken);
            user.setResetTokenExpiry(expiry);
            userRepository.save(user);

            emailService.sendPasswordResetEmail(
                    user.getEmail(),
                    user.getFullNames(),
                    resetToken,
                    appProperties.getReset().getTokenExpirationHours());
        });
    }

    @Transactional
    public AuthResponse resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByResetToken(request.getResetToken())
                .orElseThrow(() -> new ResourceNotFoundException("Invalid or expired reset token"));

        if (user.getResetTokenExpiry() == null || user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Password reset token has expired");
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException("Account is not active");
        }

        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);

        refreshTokenRepository.deleteByUser(user);

        return buildAuthResponse(user, true);
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

        String inviteToken = UUID.randomUUID().toString();
        LocalDateTime expiry = LocalDateTime.now()
                .plusHours(appProperties.getInvite().getTokenExpirationHours());

        User user = User.builder()
                .fullNames(request.getFullNames())
                .email(request.getEmail())
                .phoneNumber(PhoneUtils.normalizeRwandaPhone(request.getPhoneNumber()))
                .status(UserStatus.INVITED)
                .role(request.getRole())
                .inviteToken(inviteToken)
                .inviteTokenExpiry(expiry)
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

        emailService.sendInvitationEmail(
                user.getEmail(),
                user.getFullNames(),
                inviteToken,
                appProperties.getInvite().getTokenExpirationHours());

        return EntityMapper.toUserResponse(user);
    }

    /**
     * Generates JWT access token + DB-persisted refresh token pair.
     *
     * @param revokeExisting when true, invalidates all prior refresh tokens for this user (login / password change)
     */
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
