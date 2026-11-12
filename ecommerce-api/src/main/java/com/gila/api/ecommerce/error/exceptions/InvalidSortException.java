package com.gila.api.ecommerce.error.exceptions;

import java.util.Collection;

public final class InvalidSortException extends RuntimeException {

    public static final String FIELD = "sortBy";

    private final String sortBy;

    public InvalidSortException(String sortBy, Collection<String> allowedValues) {
        super("sortBy '" + sortBy + "' is not supported. Allowed values: " + String.join(", ", allowedValues) + ".");
        this.sortBy = sortBy;
    }

    public String getSortBy() {
        return sortBy;
    }
}
