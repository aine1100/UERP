package com.national.utility.billing.validation;

import com.national.utility.billing.dto.common.LocationAddressDto;
import com.national.utility.billing.dto.common.LocationSelectionDto;
import com.national.utility.billing.service.LocationService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ValidRwandaAddressValidator implements ConstraintValidator<ValidRwandaAddress, Object> {

    private final LocationService locationService;

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        LocationAddressDto address = toAddressDto(value);
        if (address == null) {
            return true;
        }

        if (!locationService.isLoaded()) {
            return true;
        }

        context.disableDefaultConstraintViolation();

        if (address.getProvince() == null || address.getProvince().isBlank()) {
            addViolation(context, "province", "Province is required");
            return false;
        }
        if (address.getDistrict() == null || address.getDistrict().isBlank()) {
            addViolation(context, "district", "District is required");
            return false;
        }
        if (address.getSector() == null || address.getSector().isBlank()) {
            addViolation(context, "sector", "Sector is required");
            return false;
        }
        if (address.getCell() == null || address.getCell().isBlank()) {
            addViolation(context, "cell", "Cell is required");
            return false;
        }
        if (address.getVillage() == null || address.getVillage().isBlank()) {
            addViolation(context, "village", "Village is required");
            return false;
        }

        if (!locationService.isValidAddress(address)) {
            context.buildConstraintViolationWithTemplate(
                            "Invalid location combination. Select matching province → village from the dropdowns.")
                    .addPropertyNode("village")
                    .addConstraintViolation();
            return false;
        }

        return true;
    }

    private LocationAddressDto toAddressDto(Object value) {
        if (value instanceof LocationAddressDto dto) {
            return dto;
        }
        if (value instanceof LocationSelectionDto selection) {
            return selection.toAddressDto();
        }
        return null;
    }

    private void addViolation(ConstraintValidatorContext context, String field, String message) {
        context.buildConstraintViolationWithTemplate(message)
                .addPropertyNode(field)
                .addConstraintViolation();
    }
}
