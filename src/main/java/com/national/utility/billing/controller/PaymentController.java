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

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Payments", description = "Payment processing with PDF receipt generation")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @PreAuthorize("hasAnyRole('FINANCE', 'CUSTOMER')")
    @Operation(summary = "Process payment", description = "Accept partial or full payment; sends PDF receipt via email")
    public ResponseEntity<ApiResponse<PaymentResponse>> processPayment(@Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Payment processed", paymentService.processPayment(request)));
    }

    @GetMapping
    @PreAuthorize("hasRole('FINANCE')")
    @Operation(summary = "List all payments")
    public ResponseEntity<ApiResponse<Page<PaymentResponse>>> getAllPayments(
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Payments retrieved", paymentService.getAllPayments(pageable)));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "List my payments")
    public ResponseEntity<ApiResponse<Page<PaymentResponse>>> getMyPayments(
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Payments retrieved",
                paymentService.getPaymentsForCurrentCustomer(pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('FINANCE', 'CUSTOMER')")
    @Operation(summary = "Get payment by ID")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPayment(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Payment retrieved", paymentService.getPaymentById(id)));
    }
}
