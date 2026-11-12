package com.gila.api.ecommerce.error.exceptions;

import org.jspecify.annotations.Nullable;

public class InvalidCredentialsException extends RuntimeException {

    private final @Nullable String identifier;
    private final @Nullable Object rejectedValue;

    public InvalidCredentialsException(String message) {
        this(message, null, null, null);
    }

    public InvalidCredentialsException(String message, @Nullable String identifier) {
        this(message, identifier, null, null);
    }

    public InvalidCredentialsException(String message, @Nullable String identifier, @Nullable Object rejectedValue) {
        this(message, identifier, rejectedValue, null);
    }

    public InvalidCredentialsException(
            String message,
            @Nullable String identifier,
            @Nullable Object rejectedValue,
            @Nullable Throwable cause) {
        super(message, cause);
        this.identifier = identifier;
        this.rejectedValue = rejectedValue;
    }

    public @Nullable String getIdentifier() {
        return identifier;
    }

    public @Nullable Object getRejectedValue() {
        return rejectedValue;
    }
}
