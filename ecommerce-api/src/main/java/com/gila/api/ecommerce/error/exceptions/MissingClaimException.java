package com.gila.api.ecommerce.error.exceptions;

public final class MissingClaimException extends InternalSecurityException {

    public MissingClaimException(String claimName) {
        super("JWT claim '" + claimName + "' is missing");
    }
}
