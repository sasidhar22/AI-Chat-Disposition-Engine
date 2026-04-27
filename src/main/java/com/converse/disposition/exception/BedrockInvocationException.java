package com.converse.disposition.exception;

public class BedrockInvocationException extends RuntimeException {
    public BedrockInvocationException(String message) {
        super(message);
    }

    public BedrockInvocationException(String message, Throwable cause) {
        super(message, cause);
    }
}
