package com.microform.validation.validator;

import com.microform.form.domain.FieldConstraints;

import java.util.Optional;

public class StringFieldValidator implements FieldValidator {

    @Override
    public Optional<String> validate(Object value, FieldConstraints constraints) {
        if (!(value instanceof String str)) {
            return Optional.of("must be a string");
        }
        if (constraints == null) return Optional.empty();

        if (constraints.regex() != null && !str.matches(constraints.regex())) {
            return Optional.of("does not match pattern: " + constraints.regex());
        }
        if (constraints.min() != null && str.length() < constraints.min().intValue()) {
            return Optional.of("length must be at least " + constraints.min().intValue());
        }
        if (constraints.max() != null && str.length() > constraints.max().intValue()) {
            return Optional.of("length must be at most " + constraints.max().intValue());
        }
        return Optional.empty();
    }
}
