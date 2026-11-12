package com.gila.api.ecommerce.model.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import org.jspecify.annotations.Nullable;

@Schema(description = "Product name search and structured filters, combined with AND")
public record ProductFilterParams(
        @Schema(description = "Words matched as case-insensitive substrings of the product name; "
                + "words separated by spaces are combined with OR", example = "camp tent")
        @Size(max = ProductValidationRules.SEARCH_TERM_MAX_LENGTH, message = "Name must not exceed 100 characters")
        @Nullable String name,

        @Schema(description = "SKU fragment (contains match), 3 to 10 characters [A-Z0-9]", example = "MS0")
        @Pattern(regexp = ProductValidationRules.SKU_FILTER_PATTERN, message = ProductValidationRules.SKU_FILTER_MESSAGE)
        @Nullable String sku,

        @Schema(description = "Exact category (case-insensitive)", example = "Electronics")
        @Size(max = 100, message = "Category must not exceed 100 characters")
        @Nullable String category,

        @Schema(description = "Inclusive lower price bound", example = "10.00")
        @DecimalMin(value = "0.0", message = "Minimum price must be zero or positive")
        @Nullable BigDecimal minPrice,

        @Schema(description = "Inclusive upper price bound", example = "150.00")
        @DecimalMin(value = "0.0", message = "Maximum price must be zero or positive")
        @Nullable BigDecimal maxPrice,

        @Schema(description = "Minimum stock (stock >= minStock, zero included)", example = "1")
        @PositiveOrZero(message = "Minimum stock must be zero or positive")
        @Nullable Integer minStock,

        @Schema(description = "Inclusive lower weight bound in kg; products without weight never match", example = "0.1")
        @DecimalMin(value = "0.0", message = "Minimum weight must be zero or positive")
        @Nullable BigDecimal minWeightKg,

        @Schema(description = "Inclusive upper weight bound in kg; products without weight never match", example = "2")
        @DecimalMin(value = "0.0", message = "Maximum weight must be zero or positive")
        @Nullable BigDecimal maxWeightKg) {

    public ProductFilterParams {
        name = TextNormalizer.blankToNull(name);
        category = TextNormalizer.blankToNull(category);
        sku = TextNormalizer.strip(sku);
    }

    @Schema(hidden = true)
    @AssertTrue(message = "minPrice must be less than or equal to maxPrice")
    public boolean isPriceRangeValid() {
        return minPrice == null || maxPrice == null || minPrice.compareTo(maxPrice) <= 0;
    }

    @Schema(hidden = true)
    @AssertTrue(message = "minWeightKg must be less than or equal to maxWeightKg")
    public boolean isWeightRangeValid() {
        return minWeightKg == null || maxWeightKg == null || minWeightKg.compareTo(maxWeightKg) <= 0;
    }
}
