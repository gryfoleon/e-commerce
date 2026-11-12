package com.gila.api.ecommerce.error.exceptions;

public final class InvalidPreconditionException extends RuntimeException {

    public InvalidPreconditionException(String headerValue) {
        super("Header 'If-Match' value '" + headerValue
                + "' is not a valid entity tag. Use the ETag returned by GET /api/v1/products/{id}.");
    }
}
