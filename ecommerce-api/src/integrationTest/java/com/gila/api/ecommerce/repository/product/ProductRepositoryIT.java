package com.gila.api.ecommerce.repository.product;

import static org.assertj.core.api.Assertions.assertThat;

import com.gila.api.ecommerce.configuration.IntegrationTestContainersConfiguration;
import com.gila.api.ecommerce.model.entity.Product;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("IT")
@Import(IntegrationTestContainersConfiguration.class)
class ProductRepositoryIT {

    private static final UUID LAPTOP_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID INACTIVE_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Autowired
    private ProductRepository productRepository;

    @Test
    void findByIdAndActiveTrue_returnsActiveProduct() {
        assertThat(productRepository.findByIdAndActiveTrue(LAPTOP_ID))
                .get()
                .extracting(Product::getSku, Product::getName)
                .containsExactly("LAPTOP01", "Laptop Pro 15");
    }

    @Test
    void findByIdAndActiveTrue_returnsEmptyForSoftDeletedProduct() {
        assertThat(productRepository.findByIdAndActiveTrue(INACTIVE_ID)).isEmpty();
    }

    @Test
    void findByIdAndActiveTrue_returnsEmptyForUnknownId() {
        assertThat(productRepository.findByIdAndActiveTrue(UUID.randomUUID())).isEmpty();
    }

    @Test
    void findBySkuAndActiveTrue_returnsActiveProduct() {
        assertThat(productRepository.findBySkuAndActiveTrue("MOUSE001"))
                .get()
                .extracting(Product::getId)
                .isEqualTo(UUID.fromString("22222222-2222-2222-2222-222222222222"));
    }

    @Test
    void findBySkuAndActiveTrue_returnsEmptyForSoftDeletedProduct() {
        assertThat(productRepository.findBySkuAndActiveTrue("OLDITEM1")).isEmpty();
    }

    @Test
    void findBySkuAndActiveTrue_returnsEmptyForUnknownSku() {
        assertThat(productRepository.findBySkuAndActiveTrue("NOPE")).isEmpty();
    }

    @Test
    void existsBySku_returnsTrueForActiveProduct() {
        assertThat(productRepository.existsBySku("BOOK0001")).isTrue();
    }

    @Test
    void existsBySku_returnsTrueForSoftDeletedProduct() {
        assertThat(productRepository.existsBySku("OLDITEM1")).isTrue();
    }

    @Test
    void existsBySku_returnsFalseForUnknownSku() {
        assertThat(productRepository.existsBySku("NOPE")).isFalse();
    }
}
