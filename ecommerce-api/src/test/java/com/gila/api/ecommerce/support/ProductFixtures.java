package com.gila.api.ecommerce.support;

import com.gila.api.ecommerce.model.entity.Product;
import com.gila.api.ecommerce.model.entity.ProductCategory;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.test.util.ReflectionTestUtils;

public final class ProductFixtures {

    public static final Instant CREATED_AT = Instant.parse("2026-01-01T10:00:00Z");
    public static final Instant UPDATED_AT = Instant.parse("2026-01-02T10:00:00Z");

    private ProductFixtures() {
    }

    public static ProductCategory category(short id, String name) {
        ProductCategory category = new ProductCategory(name);
        ReflectionTestUtils.setField(category, "id", id);
        ReflectionTestUtils.setField(category, "createdAt", CREATED_AT);
        return category;
    }

    public static Product product(UUID id, String sku, ProductCategory category, long version) {
        Product product = Product.create(sku, "Wireless Mouse", "Ergonomic", category,
                new BigDecimal("24.99"), 150, new BigDecimal("0.120"));
        ReflectionTestUtils.setField(product, "id", id);
        ReflectionTestUtils.setField(product, "version", version);
        ReflectionTestUtils.setField(product, "createdAt", CREATED_AT);
        ReflectionTestUtils.setField(product, "updatedAt", UPDATED_AT);
        return product;
    }
}
