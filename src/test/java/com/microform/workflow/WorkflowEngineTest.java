package com.microform.workflow;

import com.microform.form.domain.TransitionRule;
import com.microform.form.domain.WorkflowDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class WorkflowEngineTest {

    WorkflowEngine engine;

    WorkflowDefinition workflow = new WorkflowDefinition(
            List.of("DRAFT", "SUBMITTED", "UNDER_REVIEW", "APPROVED", "REJECTED"),
            List.of(
                    new TransitionRule("DRAFT", "SUBMITTED", "submitter"),
                    new TransitionRule("SUBMITTED", "UNDER_REVIEW", "reviewer"),
                    new TransitionRule("UNDER_REVIEW", "APPROVED", "reviewer"),
                    new TransitionRule("UNDER_REVIEW", "REJECTED", "reviewer")
            )
    );

    @BeforeEach
    void setUp() { engine = new WorkflowEngine(); }

    @Test
    void allowed_transition_passes() {
        assertThatNoException().isThrownBy(() ->
                engine.assertTransitionAllowed(workflow, "DRAFT", "SUBMITTED", "submitter"));
    }

    @Test
    void allowed_transition_reviewer_approve() {
        assertThatNoException().isThrownBy(() ->
                engine.assertTransitionAllowed(workflow, "UNDER_REVIEW", "APPROVED", "reviewer"));
    }

    @Test
    void wrong_role_throws() {
        assertThatThrownBy(() ->
                engine.assertTransitionAllowed(workflow, "UNDER_REVIEW", "APPROVED", "submitter"))
                .isInstanceOf(WorkflowViolationException.class)
                .hasMessageContaining("submitter");
    }

    @Test
    void undefined_transition_throws() {
        assertThatThrownBy(() ->
                engine.assertTransitionAllowed(workflow, "DRAFT", "APPROVED", "submitter"))
                .isInstanceOf(WorkflowViolationException.class);
    }

    @Test
    void reverse_transition_throws() {
        assertThatThrownBy(() ->
                engine.assertTransitionAllowed(workflow, "APPROVED", "DRAFT", "reviewer"))
                .isInstanceOf(WorkflowViolationException.class);
    }

    @Test
    void case_insensitive_role_match() {
        assertThatNoException().isThrownBy(() ->
                engine.assertTransitionAllowed(workflow, "DRAFT", "SUBMITTED", "SUBMITTER"));
    }
}
