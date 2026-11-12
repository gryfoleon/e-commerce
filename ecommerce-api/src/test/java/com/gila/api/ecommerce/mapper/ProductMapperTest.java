package com.gila.api.ecommerce.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.gila.api.ecommerce.model.dto.product.ProductCreateRequest;
import com.gila.api.ecommerce.model.dto.product.ProductResponse;
import com.gila.api.ecommerce.model.dto.product.ProductSearchDTO;
import com.gila.api.ecommerce.model.entity.Product;
import com.gila.api.ecommerce.model.entity.ProductCategory;
import com.gila.api.ecommerce.support.ProductFixtures;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProductMapperTest {

    private static final UUID ID = UUID.fromString("3f1c1a52-5d0e-4bb8-9a7a-0f6f4f3b9c11");

    private final ProductMapper mapper = new ProductMapper();
    private final ProductCategory category = ProductFixtures.category((short) 4, "Electronics");

    @Test
    void toResponse_mapsEveryFieldAndCategoryName() {
        Product product = ProductFixtures.product(ID, "ELECMS001", category, 5L);

        ProductResponse response = mapper.toResponse(product);

        assertThat(response).usingRecursiveComparison().isEqualTo(new ProductResponse(ID, "ELECMS001",
                "Wireless Mouse", "Ergonomic", "Electronics", new BigDecimal("24.99"), 150,
                new BigDecimal("0.120"), 5L, ProductFixtures.CREATED_AT, ProductFixtures.UPDATED_AT));
    }

    @Test
    void toResponse_doesNotExposeActiveOrDeletedAt() {
        assertThat(Arrays.stream(ProductResponse.class.getRecordComponents()).map(component -> component.getName()))
                .doesNotContain("active", "deletedAt");
    }

    @Test
    void toResponse_softDeletedProduct_stillMapsWithoutLeakingDeletionState() {
        Product product = ProductFixtures.product(ID, "ELECMS001", category, 1L);
        product.softDelete(ProductFixtures.UPDATED_AT);

        assertThat(mapper.toResponse(product).sku()).isEqualTo("ELECMS001");
    }

    @Test
    void toSearchDto_mapsSummaryFields() {
        Product product = ProductFixtures.product(ID, "ELECMS001", category, 5L);

        ProductSearchDTO dto = mapper.toSearchDto(product);

        assertThat(dto).usingRecursiveComparison().isEqualTo(new ProductSearchDTO(ID, "ELECMS001",
                "Wireless Mouse", "Ergonomic", "Electronics", new BigDecimal("24.99"), 150,
                new BigDecimal("0.120")));
    }

    @Test
    void toNewEntity_copiesRequestAndStartsActive() {
        ProductCreateRequest request = new ProductCreateRequest("NEW1", "Name", null, "electronics",
                new BigDecimal("1.50"), 3, null);

        Product product = mapper.toNewEntity(request, category);

        assertThat(product.getSku()).isEqualTo("NEW1");
        assertThat(product.getName()).isEqualTo("Name");
        assertThat(product.getDescription()).isNull();
        assertThat(product.getCategory()).isSameAs(category);
        assertThat(product.getPrice()).isEqualByComparingTo("1.50");
        assertThat(product.getStock()).isEqualTo(3);
        assertThat(product.getWeightKg()).isNull();
        assertThat(product.isActive()).isTrue();
        assertThat(product.getDeletedAt()).isNull();
    }
}
