package com.microform.form.domain;

import java.util.List;

public record WorkflowDefinition(
        List<String> states,
        List<TransitionRule> transitions
) {
}
