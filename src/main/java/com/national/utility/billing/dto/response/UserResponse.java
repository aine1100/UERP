package com.national.utility.billing.dto.response;

import com.national.utility.billing.model.enums.UserRole;
import com.national.utility.billing.model.enums.UserStatus;
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
@Schema(description = "User details")
public class UserResponse {

    private UUID id;
    private String fullNames;
    private String email;
    private String phoneNumber;
    private UserStatus status;
    private UserRole role;
    private LocalDateTime createdAt;
}
