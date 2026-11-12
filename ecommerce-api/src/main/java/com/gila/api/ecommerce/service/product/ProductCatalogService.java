package com.gila.api.ecommerce.service.product;

import com.gila.api.ecommerce.model.dto.PageableResponse;
import com.gila.api.ecommerce.model.dto.product.ProductFilterParams;
import com.gila.api.ecommerce.model.dto.product.ProductSearchDTO;
import org.springframework.data.domain.Pageable;

public interface ProductCatalogService {

    PageableResponse<ProductSearchDTO> filter(ProductFilterParams params, Pageable pageable);
}
