package com.flowforge.exception;

/**
 * Thrown when a task is built with invalid configuration, e.g. a blank
 * name, an unknown task type, a missing required field, or a malformed
 * value such as a non-numeric operand for a compute step.
 */
public class InvalidTaskConfigurationException extends WorkflowException {

    public InvalidTaskConfigurationException(String message) {
        super(message);
    }

    public InvalidTaskConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
