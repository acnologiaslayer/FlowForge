package com.flowforge.exception;

/**
 * Thrown when JSON text cannot be parsed, or a JSON path does not resolve to
 * a value. Part of the {@link WorkflowException} hierarchy so JSON failures
 * surface like any other domain error.
 */
public class JsonException extends WorkflowException {

    public JsonException(String message) {
        super(message);
    }

    public JsonException(String message, Throwable cause) {
        super(message, cause);
    }
}
