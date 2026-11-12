package com.gila.api.ecommerce.model.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

@Schema(description = "Product detail response")
public record ProductResponse(
        UUID id,
        String sku,
        String name,
        @Nullable String description,
        String category,
        BigDecimal price,
        Integer stock,
        @Nullable BigDecimal weightKg,
        @Schema(description = "Optimistic locking version, also exposed as the ETag header") Long version,
        Instant createdAt,
        Instant updatedAt) {
}
