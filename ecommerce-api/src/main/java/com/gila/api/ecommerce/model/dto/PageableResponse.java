package com.gila.api.ecommerce.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;

@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonPropertyOrder({"pageDetails", "content"})
public record PageableResponse<T>(List<T> content, PageDetails pageDetails) {

    public PageableResponse {
        content = Objects.requireNonNullElseGet(content, List::of);
        Objects.requireNonNull(pageDetails, "pageDetails must not be null");
    }

    public PageableResponse(PageDetails pageDetails) {
        this(List.of(), pageDetails);
    }

    public static <T> PageableResponse<T> from(Page<T> page) {
        return new PageableResponse<>(page.getContent(), new PageDetails(page.getNumber(), page.getSize(),
                page.getTotalPages(), page.getTotalElements(), page.isLast()));
    }

    @JsonPropertyOrder({"page", "size", "totalPages", "totalElements", "isLast"})
    public record PageDetails(
            int page,
            int size,
            @Nullable Integer totalPages,
            @Nullable Long totalElements,
            @JsonProperty("isLast") @Nullable Boolean isLast) {

        public PageDetails(int page, int size) {
            this(page, size, null, null, null);
        }
    }
}
