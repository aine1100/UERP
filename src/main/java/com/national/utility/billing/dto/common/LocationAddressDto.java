package com.national.utility.billing.dto.common;

import com.national.utility.billing.model.embeddable.LocationAddress;
import com.national.utility.billing.validation.ValidRwandaAddress;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ValidRwandaAddress
@Schema(description = "Resolved Rwanda address (used in location search/validate responses)")
public class LocationAddressDto {

    @NotBlank(message = "Province is required")
    @Schema(example = "KIGALI")
    private String province;

    @NotBlank(message = "District is required")
    @Schema(example = "Nyarugenge")
    private String district;

    @NotBlank(message = "Sector is required")
    @Schema(example = "Gitega")
    private String sector;

    @NotBlank(message = "Cell is required")
    @Schema(example = "Akabahizi")
    private String cell;

    @NotBlank(message = "Village is required")
    @Schema(example = "Gihanga")
    private String village;

    public static LocationAddressDto fromEntity(LocationAddress address) {
        if (address == null) {
            return null;
        }
        return LocationAddressDto.builder()
                .province(address.getProvince())
                .district(address.getDistrict())
                .sector(address.getSector())
                .cell(address.getCell())
                .village(address.getVillage())
                .build();
    }
}
