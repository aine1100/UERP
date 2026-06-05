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
@Tag(name = "Locations", description = """
        Location data from locations.json.

        **Customer / Invite forms:** use the built-in `location` dropdowns directly in those APIs.

        Helpers (optional):
        - GET /api/locations/search?keyword=Gihanga — quick lookup
        - GET /api/locations/picker — step-by-step on one URL
        """)
public class LocationController {

    private static final String SEARCH_HINT =
            "Copy the 'address' object from any result into the address field of Customer/Invite APIs";

    private final LocationService locationService;

    @GetMapping("/search")
    @Operation(summary = "Search locations (recommended)",
            description = """
                    One-call lookup. Search by village, cell, sector, district, or province name.
                    Example: keyword=Gihanga returns matching full addresses.
                    Copy the `address` JSON from a result directly into Customer/Invite requests.
                    """)
    public ResponseEntity<ApiResponse<LocationSearchResponse>> search(
            @Parameter(description = "Village, cell, sector, district, or province name", example = "Gihanga", required = true)
            @RequestParam String keyword,
            @Parameter(description = "Max results (1-100)", example = "20")
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
                results.isEmpty()
                        ? "No locations found. Try a different keyword."
                        : "Locations found — copy an address object from results",
                body));
    }

    @GetMapping("/picker")
    @Operation(summary = "Location picker (all options in one response)",
            description = """
                    Single endpoint for step-by-step selection. Each call returns ALL dropdown options
                    for the current step. Add query params progressively:
                    picker → ?province=KIGALI → &district=Nyarugenge → &sector=Gitega → &cell=Akabahizi → &village=Gihanga
                    When complete=true, copy `resolvedAddress` into Customer/Invite.
                    """)
    public ResponseEntity<ApiResponse<LocationPickerResponse>> picker(
            @Parameter(example = "KIGALI") @RequestParam(required = false) String province,
            @Parameter(example = "Nyarugenge") @RequestParam(required = false) String district,
            @Parameter(example = "Gitega") @RequestParam(required = false) String sector,
            @Parameter(example = "Akabahizi") @RequestParam(required = false) String cell,
            @Parameter(example = "Gihanga") @RequestParam(required = false) String village) {

        LocationPickerResponse response = locationService.picker(province, district, sector, cell, village);
        String message = response.isComplete()
                ? "Address complete — copy resolvedAddress"
                : "Options loaded — see nextStep for what to add";
        return ResponseEntity.ok(ApiResponse.success(message, response));
    }

    @PostMapping("/validate")
    @Operation(summary = "Validate address", description = "Optional check before submitting customer/invite")
    public ResponseEntity<ApiResponse<LocationAddressDto>> validateAddress(
            @Valid @RequestBody LocationAddressDto address) {
        return ResponseEntity.ok(ApiResponse.success("Address is valid",
                LocationAddressDto.fromEntity(locationService.resolveAddress(address))));
    }
}
