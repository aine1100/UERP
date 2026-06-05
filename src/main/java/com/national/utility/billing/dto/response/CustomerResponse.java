package com.national.utility.billing.dto.response;

import com.national.utility.billing.dto.common.LocationSelectionDto;
import com.national.utility.billing.model.enums.CustomerStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Customer details")
public class CustomerResponse {

    private UUID id;
    private String fullNames;
    private String nationalId;
    private String email;
    private String phoneNumber;
    private LocationSelectionDto location;
    private CustomerStatus status;
    private UUID userId;
    private LocalDateTime createdAt;
}
