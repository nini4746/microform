package com.microform.form.domain;

import java.util.List;

public record FieldConstraints(
        Double min,
        Double max,
        String regex,
        List<String> enumValues
) {
    public static FieldConstraints empty() {
        return new FieldConstraints(null, null, null, null);
    }
}
