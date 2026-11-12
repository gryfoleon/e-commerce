package com.gila.api.ecommerce.model.dto.product;

import org.jspecify.annotations.Nullable;

public record CsvUploadMetadata(@Nullable String filename, @Nullable String contentType, long size) {
}
