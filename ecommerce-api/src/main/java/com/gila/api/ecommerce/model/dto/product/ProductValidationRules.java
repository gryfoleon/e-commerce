package com.gila.api.ecommerce.model.dto.product;

public final class ProductValidationRules {

    public static final int SKU_MAX_LENGTH = 10;
    public static final String SKU_PATTERN = "^[A-Z0-9]{1,10}$";
    public static final String SKU_MESSAGE = "SKU must have 1 to 10 characters, only uppercase letters and digits";
    public static final String SKU_FILTER_PATTERN = "^[A-Z0-9]{3,10}$";
    public static final String SKU_FILTER_MESSAGE =
            "SKU filter must have 3 to 10 characters, only uppercase letters and digits";
    public static final int DESCRIPTION_MAX_LENGTH = 5000;
    public static final int SEARCH_TERM_MAX_LENGTH = 100;

    private ProductValidationRules() {
        throw new UnsupportedOperationException("Utility class");
    }
}
