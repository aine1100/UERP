package com.national.utility.billing.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = ValidNationalIdValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidNationalId {

    String message() default "National ID must be 16 digits starting with 1";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
