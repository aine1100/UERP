package com.national.utility.billing.dto.response;

import com.national.utility.billing.model.enums.MeterStatus;
import com.national.utility.billing.model.enums.MeterType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Meter details")
public class MeterResponse {

    private UUID id;
    private String meterNumber;
    private MeterType meterType;
    private LocalDate installationDate;
    private MeterStatus status;
    private UUID customerId;
    private String customerName;
    private LocalDateTime createdAt;
}
