package com.national.utility.billing.controller;

import com.national.utility.billing.dto.common.LocationAddressDto;
import com.national.utility.billing.dto.response.ApiResponse;
import com.national.utility.billing.dto.response.LocationPickerResponse;
import com.national.utility.billing.dto.response.LocationSearchResponse;
import com.national.utility.billing.service.LocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
@Tag(name = "Locations")
public class LocationController {

    private static final String SEARCH_HINT =
            "Copy the address object from a result into Customer/Invite location field";

    private final LocationService locationService;

    @GetMapping("/search")
    @Operation(summary = "Search locations")
    public ResponseEntity<ApiResponse<LocationSearchResponse>> search(
            @Parameter(example = "Gihanga") @RequestParam String keyword,
            @RequestParam(defaultValue = "20") int limit) {

        var results = locationService.search(keyword, limit);
        LocationSearchResponse body = LocationSearchResponse.builder()
                .keyword(keyword)
                .limit(limit)
                .count(results.size())
                .results(results)
                .hint(SEARCH_HINT)
                .build();

        return ResponseEntity.ok(ApiResponse.success(
                results.isEmpty() ? "No locations found" : "Locations found", body));
    }

    @GetMapping("/picker")
    @Operation(summary = "Location picker")
    public ResponseEntity<ApiResponse<LocationPickerResponse>> picker(
            @RequestParam(required = false) String province,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String sector,
            @RequestParam(required = false) String cell,
            @RequestParam(required = false) String village) {

        LocationPickerResponse response = locationService.picker(province, district, sector, cell, village);
        String message = response.isComplete() ? "Address complete" : "Options loaded";
        return ResponseEntity.ok(ApiResponse.success(message, response));
    }

    @PostMapping("/validate")
    @Operation(summary = "Validate address")
    public ResponseEntity<ApiResponse<LocationAddressDto>> validateAddress(
            @Valid @RequestBody LocationAddressDto address) {
        return ResponseEntity.ok(ApiResponse.success("Address is valid",
                LocationAddressDto.fromEntity(locationService.resolveAddress(address))));
    }
}
