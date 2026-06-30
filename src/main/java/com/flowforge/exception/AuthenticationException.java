package com.flowforge.exception;

/** Thrown when login, registration or credential validation fails. */
public class AuthenticationException extends WorkflowException {

    public AuthenticationException(String message) {
        super(message);
    }

    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
