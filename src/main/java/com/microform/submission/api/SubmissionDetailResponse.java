package com.microform.submission.api;

import com.microform.common.audit.AuditEvent;
import com.microform.submission.domain.Submission;
import com.microform.submission.domain.SubmissionState;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record SubmissionDetailResponse(
        UUID id,
        String formId,
        int version,
        SubmissionState state,
        String submitterId,
        Map<String, Object> data,
        List<AuditEvent> events,
        Instant createdAt,
        Instant updatedAt
) {
    public static SubmissionDetailResponse from(Submission s, List<AuditEvent> events) {
        return new SubmissionDetailResponse(
                s.getId(), s.getFormId(), s.getVersion(),
                s.getState(), s.getSubmitterId(), s.getData(),
                events, s.getCreatedAt(), s.getUpdatedAt()
        );
    }
}
