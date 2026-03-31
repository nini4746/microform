package com.microform.validation.validator;

import com.microform.form.domain.FieldConstraints;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Optional;

public class DateFieldValidator implements FieldValidator {

    @Override
    public Optional<String> validate(Object value, FieldConstraints constraints) {
        if (!(value instanceof String str)) {
            return Optional.of("must be a date string (ISO 8601: yyyy-MM-dd)");
        }
        try {
            LocalDate.parse(str);
            return Optional.empty();
        } catch (DateTimeParseException e) {
            return Optional.of("must be a valid date in format yyyy-MM-dd");
        }
    }
}
