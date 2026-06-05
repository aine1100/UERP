package com.national.utility.billing.dto.request;

import com.national.utility.billing.model.enums.UserRole;
import com.national.utility.billing.model.enums.UserStatus;
import com.national.utility.billing.validation.ValidName;
import com.national.utility.billing.validation.ValidRwandaPhone;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Update a managed user account")
public class UpdateUserRequest {

    @NotBlank(message = "Full names are required")
    @ValidName
    private String fullNames;

    @NotBlank(message = "Phone number is required")
    @ValidRwandaPhone
    private String phoneNumber;

    @NotNull(message = "Status is required")
    private UserStatus status;

    @NotNull(message = "Role is required")
    private UserRole role;
}
