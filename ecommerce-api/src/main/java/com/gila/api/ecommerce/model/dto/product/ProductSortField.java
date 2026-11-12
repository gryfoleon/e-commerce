package com.gila.api.ecommerce.model.dto.product;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;

public enum ProductSortField {
    CREATED_AT("createdAt", Sort.Direction.DESC),
    PRICE("price", Sort.Direction.ASC),
    NAME("name", Sort.Direction.ASC);

    public static final String TIE_BREAKER = "id";

    private final String property;
    private final Sort.Direction defaultDirection;

    ProductSortField(String property, Sort.Direction defaultDirection) {
        this.property = property;
        this.defaultDirection = defaultDirection;
    }

    public String property() {
        return property;
    }

    public Sort.Direction defaultDirection() {
        return defaultDirection;
    }

    public static Optional<ProductSortField> fromProperty(String value) {
        return Arrays.stream(values()).filter(field -> field.property.equalsIgnoreCase(value.strip())).findFirst();
    }

    public static List<String> properties(List<ProductSortField> fields) {
        return fields.stream().map(ProductSortField::property).toList();
    }
}
