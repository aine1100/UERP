package com.national.utility.billing.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = ValidRwandaAddressValidator.class)
@Target({ElementType.TYPE, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidRwandaAddress {

    String message() default "Address must match a valid Rwanda location from locations.json";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
