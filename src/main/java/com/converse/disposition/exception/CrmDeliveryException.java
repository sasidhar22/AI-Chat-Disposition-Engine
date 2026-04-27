package com.converse.disposition.exception;

public class CrmDeliveryException extends RuntimeException {
    public CrmDeliveryException(String message) {
        super(message);
    }

    public CrmDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
