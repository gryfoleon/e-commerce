package com.gila.api.ecommerce.model.query;

import java.math.BigDecimal;
import org.jspecify.annotations.Nullable;

public record ProductUpsertRow(
        long line,
        String sku,
        String name,
        @Nullable String description,
        short categoryId,
        BigDecimal price,
        int stock,
        @Nullable BigDecimal weightKg) {
}
