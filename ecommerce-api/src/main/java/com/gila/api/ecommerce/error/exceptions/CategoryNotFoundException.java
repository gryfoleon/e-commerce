package com.gila.api.ecommerce.error.exceptions;

public final class CategoryNotFoundException extends RuntimeException {

    private final String identifier;

    public CategoryNotFoundException(String identifier) {
        super("Category '" + identifier + "' was not found.");
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return identifier;
    }
}
