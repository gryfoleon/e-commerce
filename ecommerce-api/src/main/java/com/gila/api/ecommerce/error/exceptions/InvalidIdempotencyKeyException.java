package com.gila.api.ecommerce.error.exceptions;

public final class InvalidIdempotencyKeyException extends IdempotencyException {

    private final String idempotencyKey;

    public InvalidIdempotencyKeyException(String idempotencyKey) {
        super("Header 'Idempotency-Key' must be a valid UUID.");
        this.idempotencyKey = idempotencyKey;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }
}
