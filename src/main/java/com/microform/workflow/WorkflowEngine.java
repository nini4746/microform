package com.microform.workflow;

import com.microform.form.domain.WorkflowDefinition;
import org.springframework.stereotype.Service;

@Service
public class WorkflowEngine {

    public void assertTransitionAllowed(WorkflowDefinition workflow,
                                        String currentState,
                                        String targetState,
                                        String actorRole) {
        boolean allowed = workflow.transitions().stream()
                .anyMatch(t -> t.from().equals(currentState)
                            && t.to().equals(targetState)
                            && t.role().equalsIgnoreCase(actorRole));

        if (!allowed) {
            throw new WorkflowViolationException(currentState, targetState, actorRole);
        }
    }
}
