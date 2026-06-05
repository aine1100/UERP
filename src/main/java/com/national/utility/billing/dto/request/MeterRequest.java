package com.national.utility.billing.dto.request;

import com.national.utility.billing.model.enums.MeterStatus;
import com.national.utility.billing.model.enums.MeterType;
import com.national.utility.billing.validation.ValidMeterNumber;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ValidMeterNumber
@Schema(description = "Create or update meter")
public class MeterRequest {

    @NotBlank(message = "Meter number is required")
    @Schema(description = "REG: 11 digits | WASAC: 5-20 alphanumeric", example = "12345678901")
    private String meterNumber;

    @NotNull(message = "Meter type is required")
    @Schema(example = "ELECTRICITY")
    private MeterType meterType;

    @NotNull(message = "Installation date is required")
    @PastOrPresent(message = "Installation date cannot be in the future")
    @Schema(example = "2024-01-15")
    private LocalDate installationDate;

    @NotNull(message = "Status is required")
    @Schema(example = "ACTIVE")
    private MeterStatus status;

    @NotNull(message = "Customer ID is required")
    @Schema(example = "1")
    private Long customerId;
}
