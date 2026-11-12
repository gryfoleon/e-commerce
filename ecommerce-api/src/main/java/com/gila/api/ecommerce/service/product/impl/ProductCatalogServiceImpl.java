package com.gila.api.ecommerce.service.product.impl;

import com.gila.api.ecommerce.model.dto.PageableResponse;
import com.gila.api.ecommerce.model.dto.product.ProductFilterParams;
import com.gila.api.ecommerce.model.dto.product.ProductSearchDTO;
import com.gila.api.ecommerce.model.query.ProductFilterCriteria;
import com.gila.api.ecommerce.service.product.ProductCatalogService;
import com.gila.api.ecommerce.service.category.CategoryService;
import com.gila.api.ecommerce.service.product.ProductQueryService;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class ProductCatalogServiceImpl implements ProductCatalogService {

    private final ProductQueryService queryService;
    private final CategoryService categoryService;

    public ProductCatalogServiceImpl(
            ProductQueryService queryService,
            CategoryService categoryService) {
        this.queryService = queryService;
        this.categoryService = categoryService;
    }

    @Override
    public PageableResponse<ProductSearchDTO> filter(ProductFilterParams params, Pageable pageable) {
        ProductFilterCriteria criteria = new ProductFilterCriteria(params.name(), params.sku(), canonical(params.category()),
                params.minPrice(), params.maxPrice(), params.minStock(), params.minWeightKg(), params.maxWeightKg());
        return queryService.findPage(criteria, pageable);
    }

    private @Nullable String canonical(@Nullable String category) {
        return category == null ? null : categoryService.resolveCanonicalName(category);
    }
}
