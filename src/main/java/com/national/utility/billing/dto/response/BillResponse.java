package com.national.utility.billing.dto.response;

import com.national.utility.billing.model.enums.BillStatus;
import com.national.utility.billing.model.enums.MeterType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Bill details")
public class BillResponse {

    private UUID id;
    private String billReference;
    private BigDecimal consumption;
    private BigDecimal amount;
    private BigDecimal tax;
    private BigDecimal penalty;
    private BigDecimal totalAmount;
    private BigDecimal outstandingBalance;
    private BillStatus status;
    private Integer month;
    private Integer year;
    private UUID customerId;
    private String customerName;
    private UUID readingId;
    private MeterType utilityType;
    private LocalDateTime createdAt;
}
