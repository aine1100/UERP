package com.national.utility.billing.dto.response;

import com.national.utility.billing.model.enums.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Permissions granted to a user role")
public class RolePermissionResponse {

    private UserRole role;
    private String description;
    private List<ResourcePermissionResponse> resources;
}
