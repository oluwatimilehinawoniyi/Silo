package com.silo.common.outbox;

public class OutboxException extends RuntimeException {

    public OutboxException(String message, Throwable cause) {
        super(message, cause);
    }
}
