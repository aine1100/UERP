package com.national.utility.billing.dto.request;

import com.national.utility.billing.model.enums.PaymentMethod;
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
@Schema(description = "Process bill payment")
public class PaymentRequest {

    @NotNull(message = "Bill ID is required")
    @Positive(message = "Bill ID must be positive")
    @Schema(example = "1")
    private Long billId;

    @NotNull(message = "Amount paid is required")
    @DecimalMin(value = "0.01", inclusive = true, message = "Amount paid must be greater than zero")
    @Digits(integer = 10, fraction = 2, message = "Amount must have at most 10 integer and 2 decimal digits")
    @Schema(example = "5000.00")
    private BigDecimal amountPaid;

    @NotNull(message = "Payment method is required")
    @Schema(example = "MOBILE_MONEY")
    private PaymentMethod paymentMethod;

    @NotNull(message = "Payment date is required")
    @PastOrPresent(message = "Payment date cannot be in the future")
    @Schema(example = "2025-05-10")
    private LocalDate paymentDate;
}
