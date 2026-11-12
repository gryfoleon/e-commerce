package com.gila.api.ecommerce.error.exceptions;

public final class DuplicateSkuException extends RuntimeException {

    public static final String FIELD = "sku";

    private final String sku;

    public DuplicateSkuException(String sku) {
        this(sku, null);
    }

    public DuplicateSkuException(String sku, Throwable cause) {
        super("A product with SKU '" + sku + "' already exists.", cause);
        this.sku = sku;
    }

    public String getSku() {
        return sku;
    }
}
