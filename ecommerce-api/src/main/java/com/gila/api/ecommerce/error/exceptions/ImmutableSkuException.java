package com.gila.api.ecommerce.error.exceptions;

public final class ImmutableSkuException extends RuntimeException {

    public static final String FIELD = "sku";

    private final String requestedSku;

    public ImmutableSkuException(String currentSku, String requestedSku) {
        super("SKU is immutable. Current value '" + currentSku + "' cannot be changed to '" + requestedSku + "'.");
        this.requestedSku = requestedSku;
    }

    public String getRequestedSku() {
        return requestedSku;
    }
}
