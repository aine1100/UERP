package com.national.utility.billing.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class ValidNationalIdValidator implements ConstraintValidator<ValidNationalId, String> {

    private static final Pattern NATIONAL_ID_PATTERN = Pattern.compile("^1\\d{15}$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        return NATIONAL_ID_PATTERN.matcher(value.trim()).matches();
    }
}
