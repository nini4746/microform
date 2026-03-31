package com.microform.form.api;

import com.microform.form.domain.Form;

import java.time.Instant;

public record FormResponse(
        String formId,
        int latestVersion,
        Instant createdAt,
        Instant updatedAt
) {
    public static FormResponse from(Form form) {
        return new FormResponse(
                form.getFormId(),
                form.getLatestVersion(),
                form.getCreatedAt(),
                form.getUpdatedAt()
        );
    }
}
