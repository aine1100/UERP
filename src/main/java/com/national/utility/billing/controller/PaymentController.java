package com.national.utility.billing.controller;

import com.national.utility.billing.dto.request.PaymentRequest;
import com.national.utility.billing.dto.response.ApiResponse;
import com.national.utility.billing.dto.response.PaymentResponse;
import com.national.utility.billing.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Payments")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @PreAuthorize("@authz.adminOrAny('FINANCE', 'CUSTOMER')")
    @Operation(summary = "Submit payment (customer: pending approval; finance: approved immediately)")
    public ResponseEntity<ApiResponse<PaymentResponse>> processPayment(@Valid @RequestBody PaymentRequest request) {
        PaymentResponse payment = paymentService.processPayment(request);
        String message = payment.getStatus() == com.national.utility.billing.model.enums.PaymentStatus.PENDING_APPROVAL
                ? "Payment submitted for finance approval — bill balance updates only after finance approves"
                : "Payment processed and bill balance updated";
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(message, payment));
    }

    @GetMapping
    @PreAuthorize("@authz.adminOrAny('FINANCE')")
    @Operation(summary = "List all payments")
    public ResponseEntity<ApiResponse<Page<PaymentResponse>>> getAllPayments(
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Payments retrieved", paymentService.getAllPayments(pageable)));
    }

    @GetMapping("/my")
    @PreAuthorize("@authz.adminOrAny('CUSTOMER')")
    @Operation(summary = "List my payments")
    public ResponseEntity<ApiResponse<Page<PaymentResponse>>> getMyPayments(
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Payments retrieved",
                paymentService.getPaymentsForCurrentCustomer(pageable)));
    }

    @GetMapping("/pending")
    @PreAuthorize("@authz.adminOrAny('FINANCE')")
    @Operation(summary = "List customer payments awaiting finance approval")
    public ResponseEntity<ApiResponse<Page<PaymentResponse>>> getPendingPayments(
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                "Pending payments retrieved", paymentService.getPendingApprovalPayments(pageable)));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("@authz.adminOrAny('FINANCE')")
    @Operation(summary = "Approve customer payment — updates bill and emails receipt")
    public ResponseEntity<ApiResponse<PaymentResponse>> approvePayment(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Payment approved", paymentService.approvePayment(id)));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("@authz.adminOrAny('FINANCE')")
    @Operation(summary = "Reject customer payment")
    public ResponseEntity<ApiResponse<PaymentResponse>> rejectPayment(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Payment rejected", paymentService.rejectPayment(id)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@authz.adminOrAny('FINANCE', 'CUSTOMER')")
    @Operation(summary = "Get payment by ID")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPayment(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Payment retrieved", paymentService.getPaymentById(id)));
    }
}
