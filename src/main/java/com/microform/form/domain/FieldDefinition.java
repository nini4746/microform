package com.microform.form.domain;

public record FieldDefinition(
        String name,
        FieldType type,
        boolean required,
        FieldConstraints constraints,
        String label,
        String description
) {
}
