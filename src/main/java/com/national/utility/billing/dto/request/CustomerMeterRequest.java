package com.national.utility.billing.dto.request;

import com.national.utility.billing.model.enums.MeterType;
import com.national.utility.billing.validation.MeterNumberValidatable;
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

/**
 * Customer self-service meter registration — no customerId needed;
 * the logged-in customer's profile is resolved automatically.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ValidMeterNumber
@Schema(description = "Register a meter on your own account")
public class CustomerMeterRequest implements MeterNumberValidatable {

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
}
