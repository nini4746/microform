package com.microform.submission.api;

import com.microform.submission.domain.SubmissionState;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record CreateSubmissionRequest(
        @NotNull Map<String, Object> data,
        SubmissionState initialState  // null 이면 SUBMITTED 기본값
) {
}
