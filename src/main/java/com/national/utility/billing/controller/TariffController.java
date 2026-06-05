package com.national.utility.billing.controller;

import com.national.utility.billing.dto.request.TariffRequest;
import com.national.utility.billing.dto.response.ApiResponse;
import com.national.utility.billing.dto.response.TariffResponse;
import com.national.utility.billing.service.TariffService;
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
@RequestMapping("/api/tariffs")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Tariffs")
public class TariffController {

    private final TariffService tariffService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE')")
    @Operation(summary = "List tariffs")
    public ResponseEntity<ApiResponse<Page<TariffResponse>>> getAllTariffs(
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Tariffs retrieved", tariffService.getAllTariffs(pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE')")
    @Operation(summary = "Get tariff by ID")
    public ResponseEntity<ApiResponse<TariffResponse>> getTariff(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Tariff retrieved", tariffService.getTariffById(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create tariff")
    public ResponseEntity<ApiResponse<TariffResponse>> createTariff(@Valid @RequestBody TariffRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tariff created", tariffService.createTariff(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update tariff")
    public ResponseEntity<ApiResponse<TariffResponse>> updateTariff(
            @PathVariable Long id, @Valid @RequestBody TariffRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Tariff updated", tariffService.updateTariff(id, request)));
    }
}
