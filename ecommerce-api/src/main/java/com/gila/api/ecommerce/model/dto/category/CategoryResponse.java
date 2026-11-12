package com.gila.api.ecommerce.model.dto.category;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Category representation")
public record CategoryResponse(Short id, String name, Instant createdAt) {
}
