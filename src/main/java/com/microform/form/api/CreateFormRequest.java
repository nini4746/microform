package com.microform.form.api;

import com.microform.form.domain.FieldDefinition;
import com.microform.form.domain.WorkflowDefinition;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateFormRequest(
        @NotBlank String formId,
        @NotNull List<FieldDefinition> fields,
        @NotNull WorkflowDefinition workflow
) {
}
