package com.national.utility.billing.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Aggregated billing and collections totals for finance dashboard")
public class FinanceSummaryResponse {

    @Schema(example = "120")
    private long totalBills;

    @Schema(example = "4500000.00")
    private BigDecimal totalBilledAmount;

    @Schema(example = "850000.00")
    private BigDecimal totalOutstanding;

    @Schema(example = "25")
    private long unpaidBills;

    @Schema(example = "10")
    private long partialBills;

    @Schema(example = "85")
    private long paidBills;

    @Schema(example = "8")
    private long pendingApprovalBills;

    @Schema(example = "5")
    private long pendingApprovalPayments;

    @Schema(example = "95")
    private long totalPayments;

    @Schema(example = "3650000.00")
    private BigDecimal totalPaymentsAmount;
}
