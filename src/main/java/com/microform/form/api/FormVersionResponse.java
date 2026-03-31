package com.microform.form.api;

import com.microform.form.domain.FieldDefinition;
import com.microform.form.domain.FormVersion;
import com.microform.form.domain.WorkflowDefinition;

import java.time.Instant;
import java.util.List;

public record FormVersionResponse(
        String formId,
        int version,
        List<FieldDefinition> fields,
        WorkflowDefinition workflow,
        Instant createdAt
) {
    public static FormVersionResponse from(FormVersion fv) {
        return new FormVersionResponse(
                fv.getFormId(),
                fv.getVersion(),
                fv.getSchema(),
                fv.getWorkflow(),
                fv.getCreatedAt()
        );
    }
}
