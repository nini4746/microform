package com.microform.submission.domain;

import java.time.Instant;

public record SubmissionFilter(
        String formId,
        Integer version,
        SubmissionState state,
        Instant from,
        Instant to,
        String q,           // keyword search in data_json
        String submitterId
) {}
