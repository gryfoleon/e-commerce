package com.gila.api.ecommerce.model.dto.product;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "CSV row rejection detail")
public record CsvRowError(
        long line,
        @Nullable String sku,
        @Nullable String field,
        @Nullable String rejectedValue,
        String message) {
}
