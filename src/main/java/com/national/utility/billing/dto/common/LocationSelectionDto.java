package com.national.utility.billing.dto.common;

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
@Schema(name = "LocationSelection", description = "Select each level from the dropdown lists (loaded from locations.json)")
public class LocationSelectionDto {

    @NotBlank(message = "Province is required")
    @Schema(description = "Select province", example = "KIGALI")
    private String province;

    @NotBlank(message = "District is required")
    @Schema(description = "Select district", example = "Nyarugenge")
    private String district;

    @NotBlank(message = "Sector is required")
    @Schema(description = "Select sector", example = "Gitega")
    private String sector;

    @NotBlank(message = "Cell is required")
    @Schema(description = "Select cell", example = "Akabahizi")
    private String cell;

    @NotBlank(message = "Village is required")
    @Schema(description = "Select village", example = "Gihanga")
    private String village;

    public LocationAddressDto toAddressDto() {
        return LocationAddressDto.builder()
                .province(province)
                .district(district)
                .sector(sector)
                .cell(cell)
                .village(village)
                .build();
    }

    public static LocationSelectionDto fromAddressDto(LocationAddressDto address) {
        if (address == null) {
            return null;
        }
        return LocationSelectionDto.builder()
                .province(address.getProvince())
                .district(address.getDistrict())
                .sector(address.getSector())
                .cell(address.getCell())
                .village(address.getVillage())
                .build();
    }
}
