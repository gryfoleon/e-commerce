package com.gila.api.ecommerce.error.exceptions;

public final class AuthenticationRequiredException extends InternalSecurityException {

    public static final String DEFAULT_MESSAGE = "Authentication is required";

    public AuthenticationRequiredException() {
        this(DEFAULT_MESSAGE);
    }

    public AuthenticationRequiredException(String message) {
        super(message);
    }
}
