package com.gila.api.ecommerce.error.exceptions;

public abstract sealed class IdempotencyException extends RuntimeException
        permits MissingIdempotencyKeyException, ConcurrentIdempotencyException,
        InvalidIdempotencyKeyException, IdempotencyKeyMismatchException {

    protected IdempotencyException(String message) {
        super(message);
    }
}
