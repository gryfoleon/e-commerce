package com.gila.api.ecommerce.model.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;
import org.jspecify.annotations.Nullable;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"statusCode", "response", "timestamp", "requestDetails", "error", "errorDetails"})
public record CustomErrorResponse(
        String error,
        String response,
        int statusCode,
        String timestamp,
        @Nullable CustomRequestDetails requestDetails,
        @Nullable List<CustomErrorDetail> errorDetails) {
}
