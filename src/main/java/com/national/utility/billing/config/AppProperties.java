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
    private Reset reset = new Reset();
    private Admin admin = new Admin();

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
    public static class Reset {
        private int tokenExpirationHours;
    }

    @Data
    public static class Admin {
        private String email;
        private String fullNames;
        private String phone;
        private String password;
    }
}
