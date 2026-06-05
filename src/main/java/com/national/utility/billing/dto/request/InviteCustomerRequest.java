package com.national.utility.billing.dto.request;

import com.national.utility.billing.dto.common.LocationSelectionDto;
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
@Schema(description = "Invite a customer with billing profile")
public class InviteCustomerRequest {

    @NotBlank(message = "Full names are required")
    @ValidName
    @Schema(example = "John Doe")
    private String fullNames;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Schema(example = "john.doe@example.com")
    private String email;

    @NotBlank(message = "Phone number is required")
    @ValidRwandaPhone
    @Schema(example = "0781234567")
    private String phoneNumber;

    @NotBlank(message = "National ID is required")
    @ValidNationalId
    @Schema(example = "1199887766554433")
    private String nationalId;

    @NotNull(message = "Location is required")
    @Valid
    @Schema(description = "Customer address — select from location dropdowns")
    private LocationSelectionDto location;
}
