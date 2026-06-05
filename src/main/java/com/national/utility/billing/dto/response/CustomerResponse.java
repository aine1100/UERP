package com.national.utility.billing.dto.response;

import com.national.utility.billing.dto.common.LocationSelectionDto;
import com.national.utility.billing.model.enums.CustomerStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Customer details")
public class CustomerResponse {

    private Long id;
    private String fullNames;
    private String nationalId;
    private String email;
    private String phoneNumber;
    private LocationSelectionDto location;
    private CustomerStatus status;
    private Long userId;
    private LocalDateTime createdAt;
}
