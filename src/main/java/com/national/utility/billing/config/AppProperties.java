package com.national.utility.billing.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Jwt jwt = new Jwt();
    private Invite invite = new Invite();
    private Otp otp = new Otp();
    private Admin admin = new Admin();
    private Notifications notifications = new Notifications();

    @Data
    public static class Jwt {
        private String secret;
        private long accessTokenExpirationMs;
        private long refreshTokenExpirationMs;
    }

    @Data
    public static class Invite {
        private int tokenExpirationHours;
    }

    @Data
    public static class Otp {
        private int expirationMinutes;
    }

    @Data
    public static class Admin {
        private String email;
        private String fullNames;
        private String phone;
        private String password;
    }

    @Data
    public static class Notifications {
        /** Bills unpaid for this many days are considered overdue. */
        private int overdueDays = 30;
        /** Run overdue checks on startup and on a fixed interval. */
        private boolean overdueScheduleEnabled = true;
        /** Milliseconds between overdue checks (default 5 minutes). */
        private long overdueCheckIntervalMs = 300_000L;
    }
}
