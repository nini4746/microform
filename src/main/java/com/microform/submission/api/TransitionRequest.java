package com.microform.submission.api;

import jakarta.validation.constraints.NotBlank;

public record TransitionRequest(@NotBlank String to) {}
