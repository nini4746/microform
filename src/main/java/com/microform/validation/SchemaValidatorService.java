package com.microform.validation;

import com.microform.form.domain.FieldDefinition;
import com.microform.form.domain.FieldType;
import com.microform.validation.validator.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class SchemaValidatorService {

    private final Map<FieldType, FieldValidator> validators;

    public SchemaValidatorService() {
        validators = new EnumMap<>(FieldType.class);
        validators.put(FieldType.STRING, new StringFieldValidator());
        validators.put(FieldType.INT,    new IntFieldValidator());
        validators.put(FieldType.BOOL,   new BoolFieldValidator());
        validators.put(FieldType.DATE,   new DateFieldValidator());
        validators.put(FieldType.ENUM,   new EnumFieldValidator());
    }

    public ValidationResult validate(List<FieldDefinition> schema, Map<String, Object> data) {
        List<ValidationError> errors = new ArrayList<>();

        for (FieldDefinition field : schema) {
            Object value = data.get(field.name());

            if (value == null || "".equals(value)) {
                if (field.required()) {
                    errors.add(new ValidationError(field.name(), "is required"));
                }
                continue;
            }

            FieldValidator validator = validators.get(field.type());
            if (validator == null) {
                continue;
            }
            validator.validate(value, field.constraints())
                    .ifPresent(msg -> errors.add(new ValidationError(field.name(), msg)));
        }

        return errors.isEmpty() ? ValidationResult.ok() : ValidationResult.fail(errors);
    }
}
