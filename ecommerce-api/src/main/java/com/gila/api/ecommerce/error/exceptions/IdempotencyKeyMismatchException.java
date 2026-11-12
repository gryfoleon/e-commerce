package com.gila.api.ecommerce.error.exceptions;

public final class IdempotencyKeyMismatchException extends IdempotencyException {

    private final String idempotencyKey;

    public IdempotencyKeyMismatchException(String idempotencyKey) {
        super("Idempotency-Key '" + idempotencyKey
                + "' was already used with a different request payload. Use a new key for a different request.");
        this.idempotencyKey = idempotencyKey;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }
}
