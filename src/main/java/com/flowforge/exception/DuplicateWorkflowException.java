package com.flowforge.exception;

/**
 * Thrown when an attempt is made to create or store a workflow whose id is
 * already in use.
 */
public class DuplicateWorkflowException extends WorkflowException {

    public DuplicateWorkflowException(String workflowId) {
        super("A workflow with id '" + workflowId + "' already exists.");
    }
}
