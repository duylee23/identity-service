package com.duyle.identity_service.validator;

import com.nimbusds.jose.Payload;
import jakarta.validation.Constraint;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

//the applied target range of an annotation
@Target({ElementType.FIELD})
//when annotation being executed
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {})
public @interface DobConstraint {
    String message() default "Invalid date of birth";
    int min();
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
