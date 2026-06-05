package com.national.utility.billing.controller;

import com.national.utility.billing.dto.request.ReadingRequest;
import com.national.utility.billing.dto.response.ApiResponse;
import com.national.utility.billing.dto.response.ReadingResponse;
import com.national.utility.billing.service.ReadingService;
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
@RequestMapping("/api/readings")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Readings")
public class ReadingController {

    private final ReadingService readingService;

    @PostMapping
    @PreAuthorize("hasRole('OPERATOR')")
    @Operation(summary = "Submit reading")
    public ResponseEntity<ApiResponse<ReadingResponse>> submitReading(@Valid @RequestBody ReadingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Reading submitted and bill generated",
                        readingService.submitReading(request)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'FINANCE')")
    @Operation(summary = "List readings")
    public ResponseEntity<ApiResponse<Page<ReadingResponse>>> getAllReadings(
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Readings retrieved", readingService.getAllReadings(pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'FINANCE')")
    @Operation(summary = "Get reading by ID")
    public ResponseEntity<ApiResponse<ReadingResponse>> getReading(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Reading retrieved", readingService.getReadingById(id)));
    }
}
