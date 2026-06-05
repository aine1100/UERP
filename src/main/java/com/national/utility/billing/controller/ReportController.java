package com.national.utility.billing.controller;

import com.national.utility.billing.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Reports")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/bills/download")
    @PreAuthorize("hasRole('FINANCE')")
    @Operation(summary = "Download bills report")
    public ResponseEntity<byte[]> downloadBills(
            @RequestParam(defaultValue = "csv") String format,
            @PageableDefault(size = 100) Pageable pageable) throws IOException {

        byte[] data;
        String filename;
        MediaType mediaType;

        if ("excel".equalsIgnoreCase(format) || "xlsx".equalsIgnoreCase(format)) {
            data = reportService.exportBillsExcel(pageable);
            filename = "bills-report.xlsx";
            mediaType = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        } else {
            data = reportService.exportBillsCsv(pageable);
            filename = "bills-report.csv";
            mediaType = MediaType.parseMediaType("text/csv");
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(mediaType)
                .body(data);
    }

    @GetMapping("/payments/download")
    @PreAuthorize("hasRole('FINANCE')")
    @Operation(summary = "Download payments report")
    public ResponseEntity<byte[]> downloadPayments(
            @PageableDefault(size = 100) Pageable pageable) throws IOException {

        byte[] data = reportService.exportPaymentsCsv(pageable);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=payments-report.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(data);
    }

    @GetMapping("/customers/download")
    @PreAuthorize("hasRole('FINANCE')")
    @Operation(summary = "Download customers report")
    public ResponseEntity<byte[]> downloadCustomers(
            @PageableDefault(size = 100) Pageable pageable) throws IOException {

        byte[] data = reportService.exportCustomersCsv(pageable);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=customers-report.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(data);
    }
}
