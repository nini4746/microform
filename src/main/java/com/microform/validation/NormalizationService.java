package com.microform.validation;

import com.microform.form.domain.FieldDefinition;
import com.microform.form.domain.FieldType;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class NormalizationService {

    public void normalize(List<FieldDefinition> schema, Map<String, Object> data) {
        for (FieldDefinition field : schema) {
            Object value = data.get(field.name());
            if (value == null) continue;

            if (field.type() == FieldType.STRING && value instanceof String str) {
                data.put(field.name(), str.strip());
            }
        }
    }
}
