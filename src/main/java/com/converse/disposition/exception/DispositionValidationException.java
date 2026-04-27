package com.converse.disposition.exception;

public class DispositionValidationException extends RuntimeException {
    public DispositionValidationException(String message) {
        super(message);
    }

    public DispositionValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
