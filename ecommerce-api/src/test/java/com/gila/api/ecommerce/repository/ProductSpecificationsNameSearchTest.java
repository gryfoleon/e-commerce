package com.gila.api.ecommerce.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.gila.api.ecommerce.model.entity.Product;
import com.gila.api.ecommerce.model.entity.ProductCategory;
import com.gila.api.ecommerce.repository.category.CategoryRepository;
import com.gila.api.ecommerce.model.query.ProductFilterCriteria;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.gila.api.ecommerce.repository.product.ProductRepository;
import com.gila.api.ecommerce.repository.product.ProductSpecifications;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.PageRequest;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ProductSpecificationsNameSearchTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18");

    @Autowired
    private ProductRepository repository;

    @Autowired
    private CategoryRepository categoryRepository;

    @BeforeEach
    void seed() {
        repository.deleteAll();
        ProductCategory outdoors = categoryRepository.findByNameIgnoreCase("Outdoors").orElseThrow();
        ProductCategory electronics = categoryRepository.findByNameIgnoreCase("Electronics").orElseThrow();
        repository.save(Product.create("CP001", "Camping Chair", null, outdoors, new BigDecimal("20.00"), 5, null));
        repository.save(Product.create("CP002", "Camping Tent", null, outdoors, new BigDecimal("90.00"), 5, null));
        repository.save(Product.create("SP001", "100%_Sure Speaker", null, electronics, new BigDecimal("30.00"), 5,
                null));
        Product inactive = Product.create("CP003", "Camping Stove", null, outdoors, new BigDecimal("40.00"), 5, null);
        inactive.softDelete(Instant.now());
        repository.save(inactive);
    }

    private List<String> search(String name) {
        ProductFilterCriteria criteria = new ProductFilterCriteria(name, null, null, null, null, null, null, null);
        return repository.findAll(ProductSpecifications.from(criteria), PageRequest.of(0, 20)).stream()
                .map(Product::getSku).sorted().toList();
    }

    @Test
    void matchesPrefixAndFragmentIgnoringCase() {
        assertThat(search("camp")).containsExactly("CP001", "CP002");
        assertThat(search("CAMPING")).containsExactly("CP001", "CP002");
        assertThat(search("ten")).containsExactly("CP002");
    }

    @Test
    void wordsAreCombinedWithOr() {
        assertThat(search("camping ten")).containsExactly("CP001", "CP002");
        assertThat(search("chair speaker")).containsExactly("CP001", "SP001");
    }

    @Test
    void likeWildcardsAreTreatedAsLiterals() {
        assertThat(search("%")).containsExactly("SP001");
        assertThat(search("_")).containsExactly("SP001");
        assertThat(search("c_mp")).isEmpty();
    }

    @Test
    void typosAndInactiveProductsDoNotMatch() {
        assertThat(search("kamping")).isEmpty();
        assertThat(search("stove")).isEmpty();
    }
}
