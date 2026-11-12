package com.gila.api.ecommerce.configuration.cache;

public final class CacheNames {

    public static final String PRODUCT_DETAIL = "product-detail";

    public static final String PRODUCT_QUERY = "product-query";

    public static final String CATEGORY_DETAIL = "category-detail";

    public static final String CATEGORY_NAME = "category-name";

    public static final String CATEGORY_LIST = "category-list";

    public static final String CATEGORY_NAMES = "category-names";
    public static final String KEY_GENERATOR = "customCacheKeyGenerator";

    private CacheNames() {
        throw new UnsupportedOperationException("Utility class");
    }
}
