package com.national.utility.billing.security;

import com.national.utility.billing.config.AppProperties;
import com.national.utility.billing.model.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final AppProperties appProperties;

    public String generateAccessToken(Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return buildToken(principal.getId(), principal.getEmail(), principal.getRole().name(),
                appProperties.getJwt().getAccessTokenExpirationMs());
    }

    public String generateAccessToken(User user) {
        return buildToken(user.getId(), user.getEmail(), user.getRole().name(),
                appProperties.getJwt().getAccessTokenExpirationMs());
    }

    public String generateRefreshTokenValue() {
        return java.util.UUID.randomUUID().toString();
    }

    public long getAccessTokenExpirationMs() {
        return appProperties.getJwt().getAccessTokenExpirationMs();
    }

    public long getRefreshTokenExpirationMs() {
        return appProperties.getJwt().getRefreshTokenExpirationMs();
    }

    private String buildToken(Long userId, String email, String role, long expirationMs) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(email)
                .claim("userId", userId)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey())
                .compact();
    }

    public String getEmailFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    public Long getUserIdFromToken(String token) {
        return parseClaims(token).get("userId", Long.class);
    }

    public String getRoleFromToken(String token) {
        return parseClaims(token).get("role", String.class);
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(appProperties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8));
    }
}
