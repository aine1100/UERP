package com.national.utility.billing.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = ValidPaymentValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPayment {

    String message() default "Invalid payment submission";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
