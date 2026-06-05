package com.national.utility.billing.dto.response;

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
@Schema(description = "Meter reading details")
public class ReadingResponse {

    private Long id;
    private BigDecimal previousReading;
    private BigDecimal currentReading;
    private LocalDate readingDate;
    private Integer month;
    private Integer year;
    private Long meterId;
    private String meterNumber;
    private Long billId;
    private LocalDateTime createdAt;
}
