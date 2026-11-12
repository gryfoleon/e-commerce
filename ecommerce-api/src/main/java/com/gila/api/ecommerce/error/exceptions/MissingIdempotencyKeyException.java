package com.gila.api.ecommerce.error.exceptions;

public final class MissingIdempotencyKeyException extends IdempotencyException {

    public static final String DEFAULT_MESSAGE = "Header 'Idempotency-Key' is required and must not be blank.";

    public MissingIdempotencyKeyException() {
        this(DEFAULT_MESSAGE);
    }

    public MissingIdempotencyKeyException(String message) {
        super(message);
    }
}
