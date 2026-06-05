package com.national.utility.billing.validation;

import com.national.utility.billing.model.enums.MeterType;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class ValidMeterNumberValidator implements ConstraintValidator<ValidMeterNumber, MeterNumberValidatable> {

    private static final Pattern ELECTRICITY_PATTERN = Pattern.compile("^\\d{11}$");
    private static final Pattern WATER_PATTERN = Pattern.compile("^[A-Za-z0-9]{5,20}$");

    @Override
    public boolean isValid(MeterNumberValidatable request, ConstraintValidatorContext context) {
        if (request == null || request.getMeterNumber() == null || request.getMeterType() == null) {
            return true;
        }

        String meterNumber = request.getMeterNumber().trim();
        if (request.getMeterType() == MeterType.ELECTRICITY) {
            return ELECTRICITY_PATTERN.matcher(meterNumber).matches();
        }
        return WATER_PATTERN.matcher(meterNumber).matches();
    }
}
