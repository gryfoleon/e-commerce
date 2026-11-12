package com.gila.api.ecommerce.error.exceptions;

import java.util.Collection;

public final class InvalidCategoryException extends RuntimeException {

    public static final String FIELD = "category";

    private final String category;

    public InvalidCategoryException(String category, Collection<String> allowedCategories) {
        super("Category '" + category + "' is not supported. Allowed values: "
                + String.join(", ", allowedCategories) + ".");
        this.category = category;
    }

    public String getCategory() {
        return category;
    }
}
