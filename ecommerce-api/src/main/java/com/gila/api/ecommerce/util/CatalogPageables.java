package com.gila.api.ecommerce.util;

import com.gila.api.ecommerce.error.exceptions.InvalidSortException;
import com.gila.api.ecommerce.model.dto.CustomPageRequest;
import com.gila.api.ecommerce.model.dto.product.ProductSortField;
import java.util.List;
import org.springframework.data.domain.Pageable;

public final class CatalogPageables {

    private static final List<ProductSortField> LIST_FIELDS = List.of(ProductSortField.values());

    private CatalogPageables() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static Pageable forList(CustomPageRequest request) {
        return build(request, resolve(request, LIST_FIELDS, ProductSortField.CREATED_AT));
    }

    public static Pageable forFilter(CustomPageRequest request) {
        return forList(request);
    }

    private static ProductSortField resolve(
            CustomPageRequest request, List<ProductSortField> allowed, ProductSortField defaultField) {
        if (request.sortBy() == null) {
            return defaultField;
        }
        return ProductSortField.fromProperty(request.sortBy())
                .filter(allowed::contains)
                .orElseThrow(() -> new InvalidSortException(request.sortBy(), ProductSortField.properties(allowed)));
    }

    private static Pageable build(CustomPageRequest request, ProductSortField field) {
        return CommonUtils.buildPageable(request, field.property(), field.defaultDirection(),
                ProductSortField.TIE_BREAKER);
    }
}
