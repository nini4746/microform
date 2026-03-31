package com.microform.form.domain;

public record TransitionRule(
        String from,
        String to,
        String role
) {
}
