package com.microform.validation.validator;

import com.microform.form.domain.FieldConstraints;

import java.util.Optional;

public class IntFieldValidator implements FieldValidator {

    @Override
    public Optional<String> validate(Object value, FieldConstraints constraints) {
        double num;
        if (value instanceof Number n) {
            num = n.doubleValue();
        } else {
            try {
                num = Double.parseDouble(value.toString());
            } catch (NumberFormatException e) {
                return Optional.of("must be an integer");
            }
        }
        if (num != Math.floor(num)) {
            return Optional.of("must be an integer, not a decimal");
        }
        if (constraints == null) return Optional.empty();

        if (constraints.min() != null && num < constraints.min()) {
            return Optional.of("must be at least " + constraints.min().intValue());
        }
        if (constraints.max() != null && num > constraints.max()) {
            return Optional.of("must be at most " + constraints.max().intValue());
        }
        return Optional.empty();
    }
}
