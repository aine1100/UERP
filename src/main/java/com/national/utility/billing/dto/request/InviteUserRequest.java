package com.national.utility.billing.dto.request;

import com.national.utility.billing.dto.common.LocationSelectionDto;
import com.national.utility.billing.model.enums.UserRole;
import com.national.utility.billing.validation.ValidName;
import com.national.utility.billing.validation.ValidNationalId;
import com.national.utility.billing.validation.ValidRwandaPhone;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
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
@Schema(description = "Invite a new user (Admin only)")
public class InviteUserRequest {

    @NotBlank(message = "Full names are required")
    @ValidName
    @Schema(example = "John Doe")
    private String fullNames;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Schema(example = "john.doe@utility.gov.rw")
    private String email;

    @NotBlank(message = "Phone number is required")
    @ValidRwandaPhone
    @Schema(example = "0781234567", description = "078/079/072/073 or +25078...")
    private String phoneNumber;

    @NotNull(message = "Role is required")
    @Schema(example = "OPERATOR")
    private UserRole role;

    @ValidNationalId
    @Schema(description = "Required when inviting a CUSTOMER", example = "1199887766554433")
    private String nationalId;

    @Valid
    @Schema(description = "Required when inviting a CUSTOMER — select from dropdown lists")
    private LocationSelectionDto location;
}
