package com.national.utility.billing.model.location;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LocationRecord {

    @JsonProperty("province_name")
    private String provinceName;

    @JsonProperty("district_name")
    private String districtName;

    @JsonProperty("sector_name")
    private String sectorName;

    @JsonProperty("cell_name")
    private String cellName;

    @JsonProperty("village_name")
    private String villageName;
}
