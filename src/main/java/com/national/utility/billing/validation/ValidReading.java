package com.national.utility.billing.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = ValidReadingValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidReading {

    String message() default "Invalid reading submission";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
