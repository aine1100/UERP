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

import java.util.UUID;

@RestController
@RequestMapping("/api/bills")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Bills")
public class BillController {

    private final BillService billService;

    @GetMapping
    @PreAuthorize("@authz.adminOrAny('FINANCE')")
    @Operation(summary = "List all bills")
    public ResponseEntity<ApiResponse<Page<BillResponse>>> getAllBills(
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Bills retrieved", billService.getAllBills(pageable)));
    }

    @GetMapping("/my")
    @PreAuthorize("@authz.adminOrAny('CUSTOMER')")
    @Operation(summary = "List my bills")
    public ResponseEntity<ApiResponse<Page<BillResponse>>> getMyBills(
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Bills retrieved",
                billService.getBillsForCurrentCustomer(pageable)));
    }

    @GetMapping("/pending")
    @PreAuthorize("@authz.adminOrAny('FINANCE')")
    @Operation(summary = "List bills awaiting finance approval")
    public ResponseEntity<ApiResponse<Page<BillResponse>>> getPendingBills(
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                "Pending bills retrieved", billService.getPendingApprovalBills(pageable)));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("@authz.adminOrAny('FINANCE')")
    @Operation(summary = "Approve bill — notifies customer by email")
    public ResponseEntity<ApiResponse<BillResponse>> approveBill(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Bill approved", billService.approveBill(id)));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("@authz.adminOrAny('FINANCE')")
    @Operation(summary = "Reject bill")
    public ResponseEntity<ApiResponse<BillResponse>> rejectBill(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Bill rejected", billService.rejectBill(id)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@authz.adminOrAny('FINANCE', 'CUSTOMER')")
    @Operation(summary = "Get bill by ID")
    public ResponseEntity<ApiResponse<BillResponse>> getBill(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Bill retrieved", billService.getBillById(id)));
    }
}
