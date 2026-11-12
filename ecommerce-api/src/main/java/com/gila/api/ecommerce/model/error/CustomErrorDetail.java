package com.gila.api.ecommerce.model.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.jspecify.annotations.Nullable;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"field", "rejectedValue", "message"})
public record CustomErrorDetail(
        @Nullable String field,
        @Nullable String rejectedValue,
        @Nullable String message) {

    public CustomErrorDetail() {
        this(null, null, null);
    }
}
