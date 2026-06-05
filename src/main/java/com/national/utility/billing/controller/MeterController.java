package com.national.utility.billing.controller;

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

@RestController
@RequestMapping("/api/meters")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Meters", description = "Meter management and customer assignment")
public class MeterController {

    private final MeterService meterService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'FINANCE')")
    @Operation(summary = "List all meters", description = "Paginated list of meters")
    public ResponseEntity<ApiResponse<Page<MeterResponse>>> getAllMeters(
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Meters retrieved", meterService.getAllMeters(pageable)));
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'FINANCE')")
    @Operation(summary = "List meters by customer")
    public ResponseEntity<ApiResponse<Page<MeterResponse>>> getMetersByCustomer(
            @PathVariable Long customerId,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Meters retrieved",
                meterService.getMetersByCustomer(customerId, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'FINANCE')")
    @Operation(summary = "Get meter by ID")
    public ResponseEntity<ApiResponse<MeterResponse>> getMeter(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Meter retrieved", meterService.getMeterById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    @Operation(summary = "Create meter", description = "Assign a new meter to a customer")
    public ResponseEntity<ApiResponse<MeterResponse>> createMeter(@Valid @RequestBody MeterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Meter created", meterService.createMeter(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update meter")
    public ResponseEntity<ApiResponse<MeterResponse>> updateMeter(
            @PathVariable Long id, @Valid @RequestBody MeterRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Meter updated", meterService.updateMeter(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete meter")
    public ResponseEntity<ApiResponse<Void>> deleteMeter(@PathVariable Long id) {
        meterService.deleteMeter(id);
        return ResponseEntity.ok(ApiResponse.success("Meter deleted", null));
    }
}
