package com.national.utility.billing.dto.response;

import com.national.utility.billing.dto.common.LocationAddressDto;
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
@Schema(description = "All dropdown options for the current selection in a single response")
public class LocationPickerResponse {

    @Schema(description = "Address built from your query parameters so far")
    private LocationAddressDto selection;

    @Schema(description = "All options needed at the current step (and below when parent levels are set)")
    private PickerOptions options;

    @Schema(description = "True when province→village is complete and valid")
    private boolean complete;

    @Schema(description = "Final validated address — copy into Customer/Invite when complete=true")
    private LocationAddressDto resolvedAddress;

    @Schema(example = "Add query param district=Nyarugenge (or use GET /api/locations/search for one-step lookup)")
    private String nextStep;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PickerOptions {

        private List<String> provinces;
        private List<String> districts;
        private List<String> sectors;
        private List<String> cells;
        private List<String> villages;
    }
}
