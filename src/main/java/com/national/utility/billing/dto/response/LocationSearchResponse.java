package com.national.utility.billing.dto.response;

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
@Schema(description = "Location search results")
public class LocationSearchResponse {

    @Schema(example = "Gihanga")
    private String keyword;

    @Schema(example = "20")
    private int limit;

    @Schema(example = "15")
    private int count;

    @Schema(description = "Copy the address from any result into your Customer or Invite request")
    private List<LocationSearchResult> results;

    @Schema(example = "Copy the 'address' object from a result into the address field of Customer/Invite APIs")
    private String hint;
}
