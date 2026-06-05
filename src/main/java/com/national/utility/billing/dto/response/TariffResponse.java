package com.national.utility.billing.dto.response;

import com.national.utility.billing.model.enums.MeterType;
import com.national.utility.billing.model.enums.TariffStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Tariff details")
public class TariffResponse {

    private Long id;
    private MeterType utilityType;
    private BigDecimal ratePerUnit;
    private BigDecimal fixedServiceCharge;
    private BigDecimal vatPercentage;
    private BigDecimal latePenaltyFee;
    private Integer version;
    private LocalDate effectiveFrom;
    private TariffStatus status;
    private LocalDateTime createdAt;
}
