package com.gila.api.ecommerce.model.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

@Schema(description = "Product item summary response")
public record ProductSearchDTO(
        UUID id,
        String sku,
        String name,
        @Nullable String description,
        String category,
        BigDecimal price,
        Integer stock,
        @Nullable BigDecimal weightKg) {
}
