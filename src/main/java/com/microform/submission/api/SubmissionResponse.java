package com.microform.submission.api;

import com.microform.submission.domain.Submission;
import com.microform.submission.domain.SubmissionState;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record SubmissionResponse(
        UUID id,
        String formId,
        int version,
        SubmissionState state,
        String submitterId,
        Map<String, Object> data,
        Instant createdAt,
        Instant updatedAt
) {
    public static SubmissionResponse from(Submission s) {
        return new SubmissionResponse(
                s.getId(), s.getFormId(), s.getVersion(),
                s.getState(), s.getSubmitterId(), s.getData(),
                s.getCreatedAt(), s.getUpdatedAt()
        );
    }
}
