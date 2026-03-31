package com.microform.validation.validator;

import com.microform.form.domain.FieldConstraints;

import java.util.Optional;

public class EnumFieldValidator implements FieldValidator {

    @Override
    public Optional<String> validate(Object value, FieldConstraints constraints) {
        if (!(value instanceof String str)) {
            return Optional.of("must be a string");
        }
        if (constraints == null || constraints.enumValues() == null || constraints.enumValues().isEmpty()) {
            return Optional.empty();
        }
        if (!constraints.enumValues().contains(str)) {
            return Optional.of("must be one of: " + constraints.enumValues());
        }
        return Optional.empty();
    }
}
