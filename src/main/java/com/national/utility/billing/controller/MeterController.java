package com.national.utility.billing.controller;

import com.national.utility.billing.dto.request.CustomerMeterRequest;
import com.national.utility.billing.dto.request.MeterRequest;
import com.national.utility.billing.dto.response.ApiResponse;
import com.national.utility.billing.dto.response.MeterResponse;
import com.national.utility.billing.service.MeterService;
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
@RequestMapping("/api/meters")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Meters")
public class MeterController {

    private final MeterService meterService;

    @GetMapping
    @PreAuthorize("@authz.adminOrAny('OPERATOR', 'FINANCE')")
    @Operation(summary = "List all meters")
    public ResponseEntity<ApiResponse<Page<MeterResponse>>> getAllMeters(
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Meters retrieved", meterService.getAllMeters(pageable)));
    }

    @GetMapping("/my")
    @PreAuthorize("@authz.adminOrAny('CUSTOMER')")
    @Operation(summary = "List my meters")
    public ResponseEntity<ApiResponse<Page<MeterResponse>>> getMyMeters(
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Meters retrieved",
                meterService.getMetersForCurrentCustomer(pageable)));
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("@authz.adminOrAny('OPERATOR', 'FINANCE')")
    @Operation(summary = "List meters by customer")
    public ResponseEntity<ApiResponse<Page<MeterResponse>>> getMetersByCustomer(
            @PathVariable UUID customerId,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Meters retrieved",
                meterService.getMetersByCustomer(customerId, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@authz.adminOrAny('OPERATOR', 'FINANCE', 'CUSTOMER')")
    @Operation(summary = "Get meter by ID")
    public ResponseEntity<ApiResponse<MeterResponse>> getMeter(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Meter retrieved", meterService.getMeterById(id)));
    }

    @PostMapping("/my")
    @PreAuthorize("@authz.adminOrAny('CUSTOMER')")
    @Operation(summary = "Register a meter on my account")
    public ResponseEntity<ApiResponse<MeterResponse>> createMyMeter(
            @Valid @RequestBody CustomerMeterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Meter created", meterService.createMeterForCurrentCustomer(request)));
    }

    @PostMapping
    @PreAuthorize("@authz.adminOrAny('OPERATOR')")
    @Operation(summary = "Create meter for a customer (staff)")
    public ResponseEntity<ApiResponse<MeterResponse>> createMeter(@Valid @RequestBody MeterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Meter created", meterService.createMeter(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update meter")
    public ResponseEntity<ApiResponse<MeterResponse>> updateMeter(
            @PathVariable UUID id, @Valid @RequestBody MeterRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Meter updated", meterService.updateMeter(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete meter")
    public ResponseEntity<ApiResponse<Void>> deleteMeter(@PathVariable UUID id) {
        meterService.deleteMeter(id);
        return ResponseEntity.ok(ApiResponse.success("Meter deleted", null));
    }
}
