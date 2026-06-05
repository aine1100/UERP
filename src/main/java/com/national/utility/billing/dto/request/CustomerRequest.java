package com.national.utility.billing.dto.request;

import com.national.utility.billing.dto.common.LocationSelectionDto;
import com.national.utility.billing.model.enums.CustomerStatus;
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
@Schema(description = "Create or update customer")
public class CustomerRequest {

    @NotBlank(message = "Full names are required")
    @ValidName
    @Schema(example = "Jane Uwera")
    private String fullNames;

    @NotBlank(message = "National ID is required")
    @ValidNationalId
    @Schema(example = "1199887766554433")
    private String nationalId;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Schema(example = "jane.uwera@email.com")
    private String email;

    @NotBlank(message = "Phone number is required")
    @ValidRwandaPhone
    @Schema(example = "0789876543", description = "078/079/072/073 or +25078...")
    private String phoneNumber;

    @NotNull(message = "Location is required")
    @Valid
    @Schema(description = "Select province → village from dropdown lists")
    private LocationSelectionDto location;

    @NotNull(message = "Status is required")
    @Schema(example = "ACTIVE")
    private CustomerStatus status;
}
