package com.national.utility.billing.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * SpEL helper for {@code @PreAuthorize}. ADMIN is a super-role and may access any
 * endpoint that uses {@link #adminOrAny(String...)}.
 */
@Component("authz")
public class SecurityAuthorization {

    public boolean adminOrAny(String... roles) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String granted = authority.getAuthority();
            if ("ROLE_ADMIN".equals(granted)) {
                return true;
            }
            for (String role : roles) {
                if (("ROLE_" + role).equals(granted)) {
                    return true;
                }
            }
        }
        return false;
    }
}
