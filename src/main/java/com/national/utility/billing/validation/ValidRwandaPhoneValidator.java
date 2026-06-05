package com.national.utility.billing.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class ValidRwandaPhoneValidator implements ConstraintValidator<ValidRwandaPhone, String> {

    private static final Pattern LOCAL_PATTERN = Pattern.compile("^(078|079|072|073)\\d{7}$");
    private static final Pattern INTERNATIONAL_PATTERN = Pattern.compile("^\\+250(78|79|72|73)\\d{7}$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        String trimmed = value.trim();
        return LOCAL_PATTERN.matcher(trimmed).matches()
                || INTERNATIONAL_PATTERN.matcher(trimmed).matches();
    }
}
