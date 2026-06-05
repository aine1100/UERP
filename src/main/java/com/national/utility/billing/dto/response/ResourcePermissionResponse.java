package com.national.utility.billing.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "What a role can do on a resource")
public class ResourcePermissionResponse {

    private String resource;
    private String description;
    private boolean canView;
    private boolean canCreate;
    private boolean canUpdate;
    private boolean canDelete;
}
