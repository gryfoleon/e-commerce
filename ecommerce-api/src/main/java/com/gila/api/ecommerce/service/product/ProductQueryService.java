package com.gila.api.ecommerce.service.product;

import com.gila.api.ecommerce.model.dto.PageableResponse;
import com.gila.api.ecommerce.model.dto.product.ProductSearchDTO;
import com.gila.api.ecommerce.model.query.ProductFilterCriteria;
import org.springframework.data.domain.Pageable;

public interface ProductQueryService {

    PageableResponse<ProductSearchDTO> findPage(ProductFilterCriteria criteria, Pageable pageable);
}
