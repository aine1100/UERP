package com.national.utility.billing.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = ValidMeterNumberValidator.class)
@Target({ElementType.TYPE, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidMeterNumber {

    String message() default "Invalid meter number format for the specified meter type";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
