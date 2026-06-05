package com.national.utility.billing.dto.request;

import com.national.utility.billing.validation.ValidName;
import com.national.utility.billing.validation.ValidRwandaPhone;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Invite an operator or finance staff member")
public class InviteStaffRequest {

    @NotBlank(message = "Full names are required")
    @ValidName
    @Schema(example = "Jane Uwase")
    private String fullNames;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Schema(example = "jane.uwase@utility.gov.rw")
    private String email;

    @NotBlank(message = "Phone number is required")
    @ValidRwandaPhone
    @Schema(example = "0781234567")
    private String phoneNumber;
}
