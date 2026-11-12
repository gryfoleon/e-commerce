package com.gila.api.ecommerce.model.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import org.jspecify.annotations.Nullable;

@Schema(description = "Product creation payload")
public record ProductCreateRequest(
        @Schema(description = "Unique, immutable stock keeping unit", example = "ELECMS001")
        @NotBlank(message = "SKU is required")
        @Size(max = ProductValidationRules.SKU_MAX_LENGTH, message = "SKU must not exceed 10 characters")
        @Pattern(regexp = ProductValidationRules.SKU_PATTERN, message = ProductValidationRules.SKU_MESSAGE)
        String sku,

        @Schema(description = "Product display name", example = "Wireless Mouse")
        @NotBlank(message = "Name is required")
        @Size(max = 255, message = "Name must not exceed 255 characters")
        String name,

        @Schema(description = "Long product description", example = "Ergonomic 2.4GHz wireless mouse")
        @Size(max = ProductValidationRules.DESCRIPTION_MAX_LENGTH,
                message = "Description must not exceed " + ProductValidationRules.DESCRIPTION_MAX_LENGTH + " characters")
        @Nullable String description,

        @Schema(description = "Catalog category (case-insensitive)", example = "Electronics")
        @NotBlank(message = "Category is required")
        @Size(max = 100, message = "Category must not exceed 100 characters")
        String category,

        @Schema(description = "Unit price, strictly positive", example = "24.99")
        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.01", message = "Price must be greater than zero")
        @Digits(integer = 10, fraction = 2, message = "Price supports up to 10 integer digits and 2 decimals")
        BigDecimal price,

        @Schema(description = "Available units", example = "150")
        @NotNull(message = "Stock is required")
        @PositiveOrZero(message = "Stock must be zero or positive")
        Integer stock,

        @Schema(description = "Shipping weight in kilograms", example = "0.120")
        @PositiveOrZero(message = "Weight must be zero or positive")
        @Digits(integer = 5, fraction = 3, message = "Weight supports up to 5 integer digits and 3 decimals")
        @Nullable BigDecimal weightKg) {

    public ProductCreateRequest {
        sku = TextNormalizer.strip(sku);
        name = TextNormalizer.strip(name);
        description = TextNormalizer.blankToNull(description);
        category = TextNormalizer.strip(category);
    }
}
