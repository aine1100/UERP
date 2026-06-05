package com.national.utility.billing.dto.request;

import com.national.utility.billing.validation.ValidReading;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ValidReading
@Schema(description = "Submit meter reading")
public class ReadingRequest {

    @NotNull(message = "Meter ID is required")
    @Schema(example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID meterId;

    @NotNull(message = "Previous reading is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Previous reading cannot be negative")
    @Digits(integer = 10, fraction = 2, message = "Previous reading must have at most 10 integer and 2 decimal digits")
    @Schema(example = "1500.50", description = "Opening meter value for first reading; for later readings must match last month's current reading")
    private BigDecimal previousReading;

    @NotNull(message = "Current reading is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Current reading cannot be negative")
    @Digits(integer = 10, fraction = 2, message = "Current reading must have at most 10 integer and 2 decimal digits")
    @Schema(example = "1650.75")
    private BigDecimal currentReading;

    @NotNull(message = "Reading date is required")
    @PastOrPresent(message = "Reading date cannot be in the future")
    @Schema(example = "2025-05-01", description = "May be backdated, but not before meter installation date")
    private LocalDate readingDate;

    @NotNull(message = "Month is required")
    @Min(value = 1, message = "Month must be between 1 and 12")
    @Max(value = 12, message = "Month must be between 1 and 12")
    @Schema(example = "5")
    private Integer month;

    @NotNull(message = "Year is required")
    @Min(value = 2000, message = "Year must be 2000 or later")
    @Max(value = 2100, message = "Year must be 2100 or earlier")
    @Schema(example = "2025")
    private Integer year;
}
