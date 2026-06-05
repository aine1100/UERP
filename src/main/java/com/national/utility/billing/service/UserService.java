package com.national.utility.billing.service;

import com.national.utility.billing.config.AppProperties;
import com.national.utility.billing.dto.request.InviteCustomerRequest;
import com.national.utility.billing.dto.request.InviteStaffRequest;
import com.national.utility.billing.dto.request.UpdateUserRequest;
import com.national.utility.billing.dto.response.RolePermissionResponse;
import com.national.utility.billing.dto.response.UserResponse;
import com.national.utility.billing.exception.BusinessException;
import com.national.utility.billing.exception.ResourceNotFoundException;
import com.national.utility.billing.model.Customer;
import com.national.utility.billing.model.User;
import com.national.utility.billing.model.enums.CustomerStatus;
import com.national.utility.billing.model.enums.UserRole;
import com.national.utility.billing.model.enums.UserStatus;
import com.national.utility.billing.repository.CustomerRepository;
import com.national.utility.billing.repository.RefreshTokenRepository;
import com.national.utility.billing.repository.UserRepository;
import com.national.utility.billing.security.RolePermissionCatalog;
import com.national.utility.billing.security.SecurityUtils;
import com.national.utility.billing.security.UserPrincipal;
import com.national.utility.billing.service.mapper.EntityMapper;
import com.national.utility.billing.util.OtpGenerator;
import com.national.utility.billing.util.PhoneUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailService emailService;
    private final LocationService locationService;
    private final MeterService meterService;
    private final AppProperties appProperties;

    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(EntityMapper::toUserResponse);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID id) {
        return EntityMapper.toUserResponse(findUser(id));
    }

    @Transactional(readOnly = true)
    public List<RolePermissionResponse> getRolePermissions() {
        return RolePermissionCatalog.getAllRolePermissions();
    }

    @Transactional
    public UserResponse inviteOperator(InviteStaffRequest request) {
        return inviteStaff(request, UserRole.OPERATOR);
    }

    @Transactional
    public UserResponse inviteFinance(InviteStaffRequest request) {
        return inviteStaff(request, UserRole.FINANCE);
    }

    @Transactional
    public UserResponse inviteCustomer(InviteCustomerRequest request) {
        validateNewEmail(request.getEmail());

        if (customerRepository.existsByNationalId(request.getNationalId())) {
            throw new BusinessException("National ID is already registered");
        }
        if (customerRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Customer email is already registered");
        }

        User user = createInvitedUser(
                request.getFullNames(),
                request.getEmail(),
                request.getPhoneNumber(),
                UserRole.CUSTOMER);

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

        issueInviteOtp(user);
        return EntityMapper.toUserResponse(user);
    }

    @Transactional
    public UserResponse updateUser(UUID id, UpdateUserRequest request) {
        User user = findUser(id);
        UserPrincipal currentUser = SecurityUtils.getCurrentUser();

        validateManagedUser(user);
        validateStatusAndRole(request.getStatus(), request.getRole());

        if (user.getId().equals(currentUser.getId())) {
            if (request.getRole() != user.getRole() || request.getStatus() != user.getStatus()) {
                throw new BusinessException("You cannot change your own role or status");
            }
        }

        if (user.getRole() == UserRole.ADMIN && request.getRole() != UserRole.ADMIN) {
            ensureNotLastAdmin(user);
        }
        if (user.getRole() != UserRole.ADMIN && request.getRole() == UserRole.ADMIN) {
            throw new BusinessException("ADMIN accounts cannot be assigned through user management");
        }
        if (user.getStatus() == UserStatus.INVITED && request.getStatus() != UserStatus.INVITED) {
            throw new BusinessException("Invited users become ACTIVE only after they set their password");
        }

        user.setFullNames(request.getFullNames());
        user.setPhoneNumber(PhoneUtils.normalizeRwandaPhone(request.getPhoneNumber()));
        user.setStatus(request.getStatus());
        user.setRole(request.getRole());

        if (request.getStatus() == UserStatus.INACTIVE) {
            refreshTokenRepository.deleteByUser(user);
            if (user.getRole() == UserRole.CUSTOMER) {
                deactivateLinkedCustomer(user);
            }
        } else if (request.getStatus() == UserStatus.ACTIVE && user.getRole() == UserRole.CUSTOMER) {
            activateLinkedCustomer(user);
        }

        return EntityMapper.toUserResponse(userRepository.save(user));
    }

    @Transactional
    public void deleteUser(UUID id) {
        User user = findUser(id);
        UserPrincipal currentUser = SecurityUtils.getCurrentUser();

        validateManagedUser(user);

        if (user.getId().equals(currentUser.getId())) {
            throw new BusinessException("You cannot delete your own account");
        }
        if (user.getRole() == UserRole.ADMIN) {
            ensureNotLastAdmin(user);
        }
        if (user.getRole() == UserRole.CUSTOMER) {
            Customer customer = customerRepository.findByUserId(user.getId()).orElse(null);
            if (customer != null && !customer.getMeters().isEmpty()) {
                throw new BusinessException("Cannot delete customer with registered meters");
            }
            if (customer != null) {
                customerRepository.delete(customer);
            }
        }

        refreshTokenRepository.deleteByUser(user);
        userRepository.delete(user);
    }

    private UserResponse inviteStaff(InviteStaffRequest request, UserRole role) {
        validateNewEmail(request.getEmail());
        User user = createInvitedUser(request.getFullNames(), request.getEmail(), request.getPhoneNumber(), role);
        issueInviteOtp(user);
        return EntityMapper.toUserResponse(user);
    }

    private User createInvitedUser(String fullNames, String email, String phoneNumber, UserRole role) {
        User user = User.builder()
                .fullNames(fullNames)
                .email(email)
                .phoneNumber(PhoneUtils.normalizeRwandaPhone(phoneNumber))
                .status(UserStatus.INVITED)
                .role(role)
                .otpVerified(false)
                .build();
        return userRepository.save(user);
    }

    private void validateNewEmail(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException("Email is already registered");
        }
    }

    private void validateManagedUser(User user) {
        if (user.getRole() == UserRole.ADMIN) {
            throw new BusinessException("ADMIN accounts are managed separately and cannot be modified here");
        }
    }

    private void validateStatusAndRole(UserStatus status, UserRole role) {
        if (role == UserRole.ADMIN) {
            throw new BusinessException("ADMIN role cannot be assigned through invites or updates");
        }
        if (status == UserStatus.INVITED) {
            throw new BusinessException("Use invite endpoints to create invited users");
        }
    }

    private void ensureNotLastAdmin(User user) {
        if (user.getRole() == UserRole.ADMIN && userRepository.countByRole(UserRole.ADMIN) <= 1) {
            throw new BusinessException("Cannot remove or deactivate the last ADMIN account");
        }
    }

    private void deactivateLinkedCustomer(User user) {
        customerRepository.findByUserId(user.getId()).ifPresent(customer -> {
            customer.setStatus(CustomerStatus.INACTIVE);
            customerRepository.save(customer);
            meterService.deactivateAllForCustomer(customer.getId());
        });
    }

    private void activateLinkedCustomer(User user) {
        customerRepository.findByUserId(user.getId()).ifPresent(customer -> {
            customer.setStatus(CustomerStatus.ACTIVE);
            customerRepository.save(customer);
            meterService.activateAllForCustomer(customer.getId());
        });
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

    private User findUser(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }
}
