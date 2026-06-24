package com.flowforge.exception;

/**
 * Base class for every FlowForge domain error.
 * <p>
 * Every checked exception thrown by the model, service and persistence
 * layers extends this type, so a caller can catch all domain failures with
 * a single {@code catch (WorkflowException e)} block while still being able
 * to react to specific subtypes when it matters.
 */
public class WorkflowException extends Exception {

    public WorkflowException(String message) {
        super(message);
    }

    public WorkflowException(String message, Throwable cause) {
        super(message, cause);
    }
}
