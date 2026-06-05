package com.national.utility.billing.dto.response;

import com.national.utility.billing.model.enums.PaymentMethod;
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
@Schema(description = "Payment details")
public class PaymentResponse {

    private Long id;
    private BigDecimal amountPaid;
    private PaymentMethod paymentMethod;
    private LocalDate paymentDate;
    private Long billId;
    private String billReference;
    private BigDecimal remainingBalance;
    private LocalDateTime createdAt;
}
