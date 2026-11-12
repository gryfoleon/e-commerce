package com.gila.api.ecommerce.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Sort;

@Schema(description = "Offset pagination and sorting parameters")
public record CustomPageRequest(
        @Schema(description = "Zero-based page index", example = "0", defaultValue = "0")
        @Min(value = 0, message = "Page index must not be negative")
        @Max(value = CustomPageRequest.MAX_PAGE, message = "Page index cannot exceed 1000")
        Integer page,

        @Schema(description = "Page elements size limit", example = "20", defaultValue = "20")
        @Min(value = 1, message = "Page size must be at least 1")
        @Max(value = CustomPageRequest.MAX_SIZE, message = "Page size cannot exceed 100 elements")
        Integer size,

        @Schema(description = "Property to sort by; allowed values depend on the endpoint", example = "price")
        @Size(max = 50, message = "sortBy must not exceed 50 characters")
        @Nullable String sortBy,

        @Schema(description = "Sort direction (ASC or DESC, case-insensitive); defaults depend on the sort property",
                example = "ASC", allowableValues = {"ASC", "DESC"})
        Sort.@Nullable Direction sortDirection) {

    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_PAGE = 1000;
    public static final int MAX_SIZE = 100;

    public CustomPageRequest {
        page = Objects.requireNonNullElse(page, DEFAULT_PAGE);
        size = Objects.requireNonNullElse(size, DEFAULT_SIZE);
        sortBy = sortBy == null || sortBy.isBlank() ? null : sortBy.strip();
    }
}
