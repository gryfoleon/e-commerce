package com.gila.api.ecommerce.repository.product;

import com.gila.api.ecommerce.model.entity.Product;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {

    Optional<Product> findByIdAndActiveTrue(UUID id);

    Optional<Product> findBySkuAndActiveTrue(String sku);

    boolean existsBySku(String sku);
}
