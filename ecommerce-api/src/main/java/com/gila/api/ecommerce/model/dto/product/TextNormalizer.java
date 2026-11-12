package com.gila.api.ecommerce.model.dto.product;

import org.jspecify.annotations.Nullable;

final class TextNormalizer {

    private TextNormalizer() {
        throw new UnsupportedOperationException("Utility class");
    }

    static @Nullable String strip(@Nullable String value) {
        return value != null ? value.strip() : null;
    }

    static @Nullable String blankToNull(@Nullable String value) {
        return value != null && !value.isBlank() ? value.strip() : null;
    }
}
