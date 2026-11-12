package com.gila.api.ecommerce.error.exceptions;

public final class ProductNotFoundException extends RuntimeException {

    private final String identifier;

    public ProductNotFoundException(String identifier) {
        super("Product '" + identifier + "' was not found.");
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return identifier;
    }
}
