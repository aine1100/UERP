package com.national.utility.billing.dto.request;

import com.national.utility.billing.model.enums.MeterType;
import com.national.utility.billing.model.enums.TariffStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Create or update tariff")
public class TariffRequest {

    @NotNull(message = "Utility type is required")
    @Schema(example = "WATER")
    private MeterType utilityType;

    @NotNull(message = "Rate per unit is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Rate per unit must be greater than zero")
    @Digits(integer = 8, fraction = 4, message = "Rate per unit format is invalid")
    @Schema(example = "350.5000")
    private BigDecimal ratePerUnit;

    @NotNull(message = "Fixed service charge is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Fixed service charge cannot be negative")
    @Digits(integer = 10, fraction = 2)
    @Schema(example = "1500.00")
    private BigDecimal fixedServiceCharge;

    @NotNull(message = "VAT percentage is required")
    @DecimalMin(value = "0.0", inclusive = true)
    @DecimalMax(value = "100.0", inclusive = true, message = "VAT percentage cannot exceed 100")
    @Digits(integer = 3, fraction = 2)
    @Schema(example = "18.00")
    private BigDecimal vatPercentage;

    @NotNull(message = "Late penalty fee is required")
    @DecimalMin(value = "0.0", inclusive = true)
    @Digits(integer = 10, fraction = 2)
    @Schema(example = "500.00")
    private BigDecimal latePenaltyFee;

    @NotNull(message = "Effective from date is required")
    @Schema(example = "2025-01-01")
    private LocalDate effectiveFrom;

    @NotNull(message = "Status is required")
    @Schema(example = "ACTIVE")
    private TariffStatus status;
}
