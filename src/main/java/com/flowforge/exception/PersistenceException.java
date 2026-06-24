package com.flowforge.exception;

/**
 * Thrown when the persistence layer cannot read or write workflow files.
 * Wraps the lower-level {@link java.io.IOException} so the rest of the
 * application only ever deals with {@link WorkflowException} types.
 */
public class PersistenceException extends WorkflowException {

    public PersistenceException(String message, Throwable cause) {
        super(message, cause);
    }

    public PersistenceException(String message) {
        super(message);
    }
}
