package com.gila.api.ecommerce.error.exceptions;

public final class DuplicateCategoryException extends RuntimeException {

    public static final String FIELD = "name";

    private final String name;

    public DuplicateCategoryException(String name) {
        this(name, null);
    }

    public DuplicateCategoryException(String name, Throwable cause) {
        super("A category named '" + name + "' already exists.", cause);
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
