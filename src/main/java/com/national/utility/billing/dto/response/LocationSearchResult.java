package com.national.utility.billing.dto.response;

import com.national.utility.billing.dto.common.LocationAddressDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Searchable location result — copy the address object into Customer/Invite requests")
public class LocationSearchResult {

    @Schema(example = "KIGALI > Nyarugenge > Gitega > Akabahizi > Gihanga",
            description = "Human-readable label for quick identification")
    private String label;

    @Schema(description = "Ready-to-use address object — paste this into customer/invite address field")
    private LocationAddressDto address;
}
