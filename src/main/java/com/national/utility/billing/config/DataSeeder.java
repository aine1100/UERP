package com.national.utility.billing.config;

import com.national.utility.billing.model.User;
import com.national.utility.billing.model.enums.UserRole;
import com.national.utility.billing.model.enums.UserStatus;
import com.national.utility.billing.repository.UserRepository;
import com.national.utility.billing.service.EmailService;
import com.national.utility.billing.util.OtpGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final AppProperties appProperties;
    private final EmailService emailService;

    @Override
    public void run(String... args) {
        if (!userRepository.existsByRole(UserRole.ADMIN)) {
            seedDefaultAdmin();
        }
    }

    private void seedDefaultAdmin() {
        AppProperties.Admin adminConfig = appProperties.getAdmin();
        String otp = OtpGenerator.generateSixDigitOtp();
        LocalDateTime expiry = LocalDateTime.now()
                .plusMinutes(appProperties.getOtp().getExpirationMinutes());

        User admin = User.builder()
                .fullNames(adminConfig.getFullNames())
                .email(adminConfig.getEmail())
                .phoneNumber(adminConfig.getPhone())
                .password(null)
                .status(UserStatus.INVITED)
                .role(UserRole.ADMIN)
                .inviteToken(otp)
                .inviteTokenExpiry(expiry)
                .otpVerified(false)
                .build();

        userRepository.save(admin);

        emailService.sendVerificationOtpEmail(
                admin.getEmail(),
                admin.getFullNames(),
                otp,
                appProperties.getOtp().getExpirationMinutes());

        log.info("Default ADMIN seeded: {}", admin.getEmail());
        log.info("Admin OTP (dev): {}", otp);
    }
}
