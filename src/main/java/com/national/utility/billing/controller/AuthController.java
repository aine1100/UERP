package com.national.utility.billing.controller;

import com.national.utility.billing.dto.request.*;
import com.national.utility.billing.dto.response.ApiResponse;
import com.national.utility.billing.dto.response.AuthResponse;
import com.national.utility.billing.dto.response.MessageResponse;
import com.national.utility.billing.dto.response.VerifyAccountResponse;
import com.national.utility.billing.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Login successful", authService.login(request)));
    }

    @PostMapping("/verify-account")
    @Operation(summary = "Verify account with OTP")
    public ResponseEntity<ApiResponse<VerifyAccountResponse>> verifyAccount(
            @Valid @RequestBody VerifyAccountRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Account verified", authService.verifyAccount(request)));
    }

    @PostMapping("/resend-otp")
    @Operation(summary = "Resend verification OTP")
    public ResponseEntity<ApiResponse<Void>> resendOtp(@Valid @RequestBody ResendOtpRequest request) {
        authService.resendOtp(request);
        return ResponseEntity.ok(ApiResponse.success(
                "If an invited account exists for that email, a new OTP has been sent.", null));
    }

    @PostMapping("/setup-password")
    @Operation(summary = "Setup password")
    public ResponseEntity<ApiResponse<MessageResponse>> setupPassword(
            @Valid @RequestBody SetupPasswordRequest request) {
        MessageResponse response = authService.setupPassword(request);
        return ResponseEntity.ok(ApiResponse.success(response.getMessage(), response));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Forgot password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success(
                "If an active account exists for that email, a reset OTP has been sent.", null));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password")
    public ResponseEntity<ApiResponse<MessageResponse>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        MessageResponse response = authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success(response.getMessage(), response));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh token")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Token refreshed", authService.refreshToken(request)));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request);
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully", null));
    }

}
