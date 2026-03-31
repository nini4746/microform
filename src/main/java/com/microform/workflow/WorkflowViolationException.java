package com.microform.workflow;

public class WorkflowViolationException extends RuntimeException {

    public WorkflowViolationException(String currentState, String targetState, String role) {
        super("Transition from [" + currentState + "] to [" + targetState +
              "] is not allowed for role [" + role + "]");
    }
}
