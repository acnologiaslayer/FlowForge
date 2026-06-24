package com.flowforge.exception;

/**
 * Thrown when a workflow is requested by an id that does not exist in the
 * repository.
 */
public class WorkflowNotFoundException extends WorkflowException {

    public WorkflowNotFoundException(String workflowId) {
        super("No workflow found with id: " + workflowId);
    }
}
