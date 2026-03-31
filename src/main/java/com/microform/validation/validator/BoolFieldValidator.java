package com.microform.validation.validator;

import com.microform.form.domain.FieldConstraints;

import java.util.Optional;

public class BoolFieldValidator implements FieldValidator {

    @Override
    public Optional<String> validate(Object value, FieldConstraints constraints) {
        if (value instanceof Boolean) return Optional.empty();
        if (value instanceof String s) {
            if (s.equalsIgnoreCase("true") || s.equalsIgnoreCase("false")) return Optional.empty();
        }
        return Optional.of("must be a boolean (true/false)");
    }
}
