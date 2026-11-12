package com.gila.api.ecommerce.error.exceptions;

public final class InvalidTokenException extends InternalSecurityException {

    public InvalidTokenException(String details) {
        super("Invalid or expired token: " + details);
    }
}
