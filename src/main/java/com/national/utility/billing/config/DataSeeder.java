package com.national.utility.billing.config;

import com.national.utility.billing.model.User;
import com.national.utility.billing.model.enums.UserRole;
import com.national.utility.billing.model.enums.UserStatus;
import com.national.utility.billing.repository.UserRepository;
import com.national.utility.billing.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

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
        String inviteToken = UUID.randomUUID().toString();
        LocalDateTime expiry = LocalDateTime.now().plusHours(appProperties.getInvite().getTokenExpirationHours());

        User admin = User.builder()
                .fullNames(adminConfig.getFullNames())
                .email(adminConfig.getEmail())
                .phoneNumber(adminConfig.getPhone())
                .password(null)
                .status(UserStatus.INVITED)
                .role(UserRole.ADMIN)
                .inviteToken(inviteToken)
                .inviteTokenExpiry(expiry)
                .build();

        userRepository.save(admin);

        emailService.sendInvitationEmail(
                admin.getEmail(),
                admin.getFullNames(),
                inviteToken,
                appProperties.getInvite().getTokenExpirationHours());

        log.info("Default ADMIN seeded with email: {}. Invite token sent by email.", admin.getEmail());
        log.info("Admin invite token (dev): {}", inviteToken);
    }
}
