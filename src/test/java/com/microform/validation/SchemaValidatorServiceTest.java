package com.microform.validation;

import com.microform.form.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class SchemaValidatorServiceTest {

    SchemaValidatorService validator;

    @BeforeEach
    void setUp() { validator = new SchemaValidatorService(); }

    private FieldDefinition field(String name, FieldType type, boolean required) {
        return new FieldDefinition(name, type, required, FieldConstraints.empty(), name, null);
    }

    private FieldDefinition fieldWithConstraints(String name, FieldType type, boolean required, FieldConstraints constraints) {
        return new FieldDefinition(name, type, required, constraints, name, null);
    }

    // --- required ---
    @Test
    void required_field_missing_fails() {
        var schema = List.of(field("email", FieldType.STRING, true));
        var result = validator.validate(schema, Map.of());
        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).anyMatch(e -> e.field().equals("email") && e.message().contains("required"));
    }

    @Test
    void optional_field_missing_passes() {
        var schema = List.of(field("nickname", FieldType.STRING, false));
        var result = validator.validate(schema, Map.of());
        assertThat(result.valid()).isTrue();
    }

    // --- STRING ---
    @Test
    void string_valid() {
        var schema = List.of(field("name", FieldType.STRING, true));
        assertThat(validator.validate(schema, Map.of("name", "Alice")).valid()).isTrue();
    }

    @Test
    void string_regex_valid() {
        var constraints = new FieldConstraints(null, null, "^[^@]+@[^@]+\\.[^@]+$", null);
        var schema = List.of(fieldWithConstraints("email", FieldType.STRING, true, constraints));
        assertThat(validator.validate(schema, Map.of("email", "user@example.com")).valid()).isTrue();
    }

    @Test
    void string_regex_invalid() {
        var constraints = new FieldConstraints(null, null, "^[^@]+@[^@]+\\.[^@]+$", null);
        var schema = List.of(fieldWithConstraints("email", FieldType.STRING, true, constraints));
        var result = validator.validate(schema, Map.of("email", "not-an-email"));
        assertThat(result.valid()).isFalse();
    }

    @Test
    void string_max_length_exceeded() {
        var constraints = new FieldConstraints(null, 5.0, null, null);
        var schema = List.of(fieldWithConstraints("code", FieldType.STRING, true, constraints));
        var result = validator.validate(schema, Map.of("code", "toolong"));
        assertThat(result.valid()).isFalse();
    }

    // --- INT ---
    @Test
    void int_valid() {
        var schema = List.of(field("age", FieldType.INT, true));
        assertThat(validator.validate(schema, Map.of("age", 25)).valid()).isTrue();
    }

    @Test
    void int_min_constraint_fails() {
        var constraints = new FieldConstraints(0.0, 150.0, null, null);
        var schema = List.of(fieldWithConstraints("age", FieldType.INT, true, constraints));
        var result = validator.validate(schema, Map.of("age", -1));
        assertThat(result.valid()).isFalse();
    }

    @Test
    void int_max_constraint_fails() {
        var constraints = new FieldConstraints(0.0, 150.0, null, null);
        var schema = List.of(fieldWithConstraints("age", FieldType.INT, true, constraints));
        var result = validator.validate(schema, Map.of("age", 200));
        assertThat(result.valid()).isFalse();
    }

    @Test
    void int_non_integer_fails() {
        var schema = List.of(field("count", FieldType.INT, true));
        var result = validator.validate(schema, Map.of("count", "abc"));
        assertThat(result.valid()).isFalse();
    }

    // --- BOOL ---
    @Test
    void bool_true_passes() {
        var schema = List.of(field("tos", FieldType.BOOL, true));
        assertThat(validator.validate(schema, Map.of("tos", true)).valid()).isTrue();
    }

    @Test
    void bool_string_true_passes() {
        var schema = List.of(field("tos", FieldType.BOOL, true));
        assertThat(validator.validate(schema, Map.of("tos", "true")).valid()).isTrue();
    }

    @Test
    void bool_invalid_fails() {
        var schema = List.of(field("tos", FieldType.BOOL, true));
        var result = validator.validate(schema, Map.of("tos", "yes"));
        assertThat(result.valid()).isFalse();
    }

    // --- DATE ---
    @Test
    void date_valid_iso() {
        var schema = List.of(field("birthday", FieldType.DATE, true));
        assertThat(validator.validate(schema, Map.of("birthday", "2000-01-15")).valid()).isTrue();
    }

    @Test
    void date_invalid_format_fails() {
        var schema = List.of(field("birthday", FieldType.DATE, true));
        var result = validator.validate(schema, Map.of("birthday", "15/01/2000"));
        assertThat(result.valid()).isFalse();
    }

    // --- ENUM ---
    @Test
    void enum_valid_value() {
        var constraints = new FieldConstraints(null, null, null, List.of("RED", "GREEN", "BLUE"));
        var schema = List.of(fieldWithConstraints("color", FieldType.ENUM, true, constraints));
        assertThat(validator.validate(schema, Map.of("color", "RED")).valid()).isTrue();
    }

    @Test
    void enum_invalid_value_fails() {
        var constraints = new FieldConstraints(null, null, null, List.of("RED", "GREEN", "BLUE"));
        var schema = List.of(fieldWithConstraints("color", FieldType.ENUM, true, constraints));
        var result = validator.validate(schema, Map.of("color", "YELLOW"));
        assertThat(result.valid()).isFalse();
    }

    // --- multiple errors ---
    @Test
    void multiple_field_errors_collected() {
        var schema = List.of(
                field("name", FieldType.STRING, true),
                field("age", FieldType.INT, true)
        );
        var result = validator.validate(schema, Map.of());
        assertThat(result.errors()).hasSize(2);
    }
}
