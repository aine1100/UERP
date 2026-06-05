package com.national.utility.billing.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = ValidRwandaPhoneValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidRwandaPhone {

    String message() default "Phone must be a valid Rwanda number (078/079/072/073 or +25078/79/72/73)";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
