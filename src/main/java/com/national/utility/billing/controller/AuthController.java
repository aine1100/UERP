package com.national.utility.billing.controller;

import com.national.utility.billing.dto.request.*;
import com.national.utility.billing.dto.response.ApiResponse;
import com.national.utility.billing.dto.response.AuthResponse;
import com.national.utility.billing.dto.response.UserResponse;
import com.national.utility.billing.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = """
        Login, password setup, password reset, token refresh, and user invitations.

        Token storage:
        - Access Token (JWT): generated on login — stateless, NOT stored in DB; send as Bearer header.
        - Refresh Token: generated on login — stored in refresh_tokens table; use POST /refresh to renew.
        - Invite Token: stored on users table until setup-password (INVITED → ACTIVE).
        - Reset Token: stored on users table until reset-password (forgot-password flow).
        """)
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Login",
            description = "Validates credentials and returns a new JWT access token + refresh token (saved in DB)")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Login successful", authService.login(request)));
    }

    @PostMapping("/setup-password")
    @Operation(summary = "Setup password",
            description = "Activate invited account (including ADMIN) using invitation token; returns tokens")
    public ResponseEntity<ApiResponse<AuthResponse>> setupPassword(@Valid @RequestBody SetupPasswordRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Password set successfully. Account is now active.",
                authService.setupPassword(request)));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Forgot password",
            description = "Sends reset token to email if account is ACTIVE. Use token with POST /reset-password. Always returns success.")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success(
                "If an active account exists for that email, a password reset token has been sent.", null));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password",
            description = "Set new password using reset token from email; invalidates old refresh tokens and returns new tokens")
    public ResponseEntity<ApiResponse<AuthResponse>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Password reset successful",
                authService.resetPassword(request)));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh token",
            description = "Exchange a valid refresh token (from DB) for a new access + refresh token pair")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Token refreshed", authService.refreshToken(request)));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout",
            description = "Revokes the provided refresh token from the database")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request);
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully", null));
    }

    @PostMapping("/invite")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Invite user", description = "Admin invites Operator, Finance, or Customer users")
    public ResponseEntity<ApiResponse<UserResponse>> inviteUser(@Valid @RequestBody InviteUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User invited successfully", authService.inviteUser(request)));
    }
}
