package com.microform.validation.validator;

import com.microform.form.domain.FieldConstraints;

import java.util.Optional;

@FunctionalInterface
public interface FieldValidator {
    Optional<String> validate(Object value, FieldConstraints constraints);
}
