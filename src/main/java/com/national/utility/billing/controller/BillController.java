package com.national.utility.billing.controller;

import com.national.utility.billing.dto.response.ApiResponse;
import com.national.utility.billing.dto.response.BillResponse;
import com.national.utility.billing.service.BillService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bills")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Bills", description = "Bill viewing and management")
public class BillController {

    private final BillService billService;

    @GetMapping
    @PreAuthorize("hasRole('FINANCE')")
    @Operation(summary = "List all bills", description = "Finance views all bills (paginated)")
    public ResponseEntity<ApiResponse<Page<BillResponse>>> getAllBills(
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Bills retrieved", billService.getAllBills(pageable)));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "List my bills", description = "Customer views own bills")
    public ResponseEntity<ApiResponse<Page<BillResponse>>> getMyBills(
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Bills retrieved",
                billService.getBillsForCurrentCustomer(pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('FINANCE', 'CUSTOMER')")
    @Operation(summary = "Get bill by ID")
    public ResponseEntity<ApiResponse<BillResponse>> getBill(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Bill retrieved", billService.getBillById(id)));
    }
}
