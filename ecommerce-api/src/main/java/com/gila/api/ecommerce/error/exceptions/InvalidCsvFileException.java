package com.gila.api.ecommerce.error.exceptions;

public final class InvalidCsvFileException extends RuntimeException {

    public InvalidCsvFileException(String message) {
        super(message);
    }

    public InvalidCsvFileException(String message, Throwable cause) {
        super(message, cause);
    }
}
