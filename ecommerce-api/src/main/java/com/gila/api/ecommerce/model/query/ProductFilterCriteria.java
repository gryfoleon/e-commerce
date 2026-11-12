package com.gila.api.ecommerce.model.query;

import java.math.BigDecimal;
import org.jspecify.annotations.Nullable;

public record ProductFilterCriteria(
        @Nullable String name,
        @Nullable String sku,
        @Nullable String category,
        @Nullable BigDecimal minPrice,
        @Nullable BigDecimal maxPrice,
        @Nullable Integer minStock,
        @Nullable BigDecimal minWeightKg,
        @Nullable BigDecimal maxWeightKg) {
}
