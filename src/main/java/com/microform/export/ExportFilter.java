package com.microform.export;

import com.microform.submission.domain.SubmissionState;

import java.time.Instant;

public record ExportFilter(
        String formId,
        int version,
        Instant from,
        Instant to,
        SubmissionState state
) {}
