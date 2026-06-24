package com.flowforge.exception;

/**
 * Thrown when a workflow itself is invalid, e.g. it has a blank name or no
 * steps when one is required to run.
 */
public class WorkflowValidationException extends WorkflowException {

    public WorkflowValidationException(String message) {
        super(message);
    }
}
