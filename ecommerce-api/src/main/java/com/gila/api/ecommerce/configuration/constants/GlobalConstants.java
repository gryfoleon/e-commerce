package com.gila.api.ecommerce.configuration.constants;

public final class GlobalConstants {

    public static final String GENERAL_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";
    public static final String AMERICA_MEXICO_CITY_ZONE_ID = "America/Mexico_City";

    public static final String SYSTEM_USERNAME = "SYSTEM";

    public static final String GENERAL_ERROR = "An unexpected error occurred";
    public static final String VALIDATION_ERROR = "Validation error";

    public static final String BAD_CREDENTIALS = "Bad Credentials";
    public static final String AUTHENTICATION_ERROR = "Authentication Failed";
    public static final String TOKEN_EXPIRED = "Token Expired";
    public static final String ACCESS_DENIED = "Access Denied";

    public static final String RESOURCE_NOT_FOUND = "Resource not found";
    public static final String RESOURCE_CONFLICT = "Resource conflict";
    public static final String UNPROCESSABLE_CONTENT = "Unprocessable content";
    public static final String IDEMPOTENCY_ERROR = "Idempotency error";
    public static final String INGESTION_ERROR = "CSV ingestion error";

    public static final String API_V1_PRODUCTS = "/v1/products";
    public static final String API_V1_CATEGORIES = "/v1/categories";

    public static final String HEADER_IDEMPOTENCY_KEY = "Idempotency-Key";
    public static final String HEADER_IDEMPOTENT_REPLAYED = "Idempotent-Replayed";

    public static final long PRODUCT_FILTER_CACHE_MAX_AGE_SECONDS = 30;
    public static final long PRODUCT_DETAIL_CACHE_MAX_AGE_SECONDS = 60;

    private GlobalConstants() {
        throw new UnsupportedOperationException("Utility class");
    }
}
