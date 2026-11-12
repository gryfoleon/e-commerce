package com.gila.api.ecommerce.error.exceptions;

import org.jspecify.annotations.Nullable;

public abstract sealed class InternalSecurityException extends RuntimeException
        permits AuthenticationRequiredException, InvalidTokenException, MissingClaimException {

    protected InternalSecurityException(String message) {
        this(message, null);
    }

    protected InternalSecurityException(String message, @Nullable Throwable cause) {
        super(message, cause);
    }
}
