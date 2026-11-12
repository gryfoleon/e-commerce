package com.gila.api.ecommerce.error.exceptions;

import java.util.Objects;

public final class ConcurrentIdempotencyException extends IdempotencyException {

    private final String idempotencyKey;

    public ConcurrentIdempotencyException(String idempotencyKey) {
        this(idempotencyKey,
                "A request with Idempotency-Key '" + idempotencyKey + "' is currently processing in parallel.");
    }

    public ConcurrentIdempotencyException(String idempotencyKey, String message) {
        super(message);
        this.idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }
}
