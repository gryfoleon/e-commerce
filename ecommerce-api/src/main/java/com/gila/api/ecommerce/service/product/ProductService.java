package com.gila.api.ecommerce.service.product;

import com.gila.api.ecommerce.model.dto.product.ProductCreateRequest;
import com.gila.api.ecommerce.model.dto.product.ProductResponse;
import com.gila.api.ecommerce.model.dto.product.ProductUpdateRequest;
import java.util.Optional;
import java.util.UUID;

public interface ProductService {

    ProductResponse findById(UUID id);

    ProductResponse create(ProductCreateRequest request);

    ProductResponse update(String idOrSku, ProductUpdateRequest request, Optional<Long> expectedVersion);

    UUID delete(String idOrSku);
}
