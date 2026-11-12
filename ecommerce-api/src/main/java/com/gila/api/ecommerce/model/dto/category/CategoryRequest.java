package com.gila.api.ecommerce.model.dto.category;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Category payload")
public record CategoryRequest(
        @Schema(description = "Unique category name (case-insensitive)", example = "Electronics")
        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must not exceed 100 characters")
        String name) {

    public CategoryRequest {
        name = name != null ? name.strip() : null;
    }
}
