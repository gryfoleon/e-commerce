package com.gila.api.ecommerce.mapper;

import com.gila.api.ecommerce.model.dto.product.ProductCreateRequest;
import com.gila.api.ecommerce.model.dto.product.ProductResponse;
import com.gila.api.ecommerce.model.dto.product.ProductSearchDTO;
import com.gila.api.ecommerce.model.entity.Product;
import com.gila.api.ecommerce.model.entity.ProductCategory;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    public Product toNewEntity(ProductCreateRequest request, ProductCategory category) {
        return Product.create(request.sku(), request.name(), request.description(), category,
                request.price(), request.stock(), request.weightKg());
    }

    public ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getDescription(),
                product.getCategory().getName(),
                product.getPrice(),
                product.getStock(),
                product.getWeightKg(),
                product.getVersion(),
                product.getCreatedAt(),
                product.getUpdatedAt());
    }

    public ProductSearchDTO toSearchDto(Product product) {
        return new ProductSearchDTO(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getDescription(),
                product.getCategory().getName(),
                product.getPrice(),
                product.getStock(),
                product.getWeightKg());
    }
}
