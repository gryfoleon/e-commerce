package com.gila.api.ecommerce.service.product.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.gila.api.ecommerce.error.exceptions.DuplicateSkuException;
import com.gila.api.ecommerce.error.exceptions.ImmutableSkuException;
import com.gila.api.ecommerce.error.exceptions.InvalidCategoryException;
import com.gila.api.ecommerce.error.exceptions.ProductNotFoundException;
import com.gila.api.ecommerce.mapper.ProductMapper;
import com.gila.api.ecommerce.model.dto.product.ProductCreateRequest;
import com.gila.api.ecommerce.model.dto.product.ProductResponse;
import com.gila.api.ecommerce.model.dto.product.ProductUpdateRequest;
import com.gila.api.ecommerce.model.entity.Product;
import com.gila.api.ecommerce.model.entity.ProductCategory;
import com.gila.api.ecommerce.repository.product.ProductRepository;
import com.gila.api.ecommerce.service.category.CategoryService;
import com.gila.api.ecommerce.support.ProductFixtures;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-03-04T05:06:07Z");
    private static final UUID ID = UUID.fromString("3f1c1a52-5d0e-4bb8-9a7a-0f6f4f3b9c11");

    @Mock
    private ProductRepository productRepository;
    @Mock
    private CategoryService categoryService;

    private ProductServiceImpl service;
    private ProductCategory electronics;
    private Product existing;

    @BeforeEach
    void setUp() {
        service = new ProductServiceImpl(productRepository, categoryService, new ProductMapper(),
                Clock.fixed(NOW, ZoneOffset.UTC));
        electronics = ProductFixtures.category((short) 1, "Electronics");
        existing = ProductFixtures.product(ID, "ELECMS001", electronics, 3L);
    }

    private static ProductCreateRequest createRequest(String category) {
        return new ProductCreateRequest("ELECMS001", "Wireless Mouse", "Ergonomic", category,
                new BigDecimal("24.99"), 150, new BigDecimal("0.120"));
    }

    private static ProductUpdateRequest updateRequest(String sku) {
        return new ProductUpdateRequest(sku, "Mouse Pro", null, "electronics", new BigDecimal("30.00"), 90, null);
    }

    private static DataIntegrityViolationException violation(String message) {
        return new DataIntegrityViolationException("could not execute", new SQLException(message));
    }

    @Test
    void findById_activeProduct_returnsMappedResponse() {
        given(productRepository.findByIdAndActiveTrue(ID)).willReturn(Optional.of(existing));

        ProductResponse response = service.findById(ID);

        assertThat(response).usingRecursiveComparison().isEqualTo(new ProductResponse(ID, "ELECMS001",
                "Wireless Mouse", "Ergonomic", "Electronics", new BigDecimal("24.99"), 150,
                new BigDecimal("0.120"), 3L, ProductFixtures.CREATED_AT, ProductFixtures.UPDATED_AT));
    }

    @Test
    void findById_missingOrSoftDeleted_throwsNotFound() {
        given(productRepository.findByIdAndActiveTrue(ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(ID)).isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void findById_missing_neverLooksUpBySku() {
        given(productRepository.findByIdAndActiveTrue(ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(ID)).isInstanceOf(ProductNotFoundException.class);

        verify(productRepository, never()).findBySkuAndActiveTrue(any());
    }

    @Test
    void create_validRequest_persistsWithCanonicalCategory() {
        given(categoryService.getByName("electronics")).willReturn(electronics);
        given(productRepository.existsBySku("ELECMS001")).willReturn(false);
        given(productRepository.saveAndFlush(any(Product.class))).willAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            return ProductFixtures.product(ID, product.getSku(), product.getCategory(), 0L);
        });

        ProductResponse response = service.create(createRequest("electronics"));

        ArgumentCaptor<Product> saved = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getCategory()).isSameAs(electronics);
        assertThat(response.category()).isEqualTo("Electronics");
        assertThat(response.id()).isEqualTo(ID);
    }

    @Test
    void create_existingSku_throwsDuplicateWithoutSaving() {
        given(categoryService.getByName("Electronics")).willReturn(electronics);
        given(productRepository.existsBySku("ELECMS001")).willReturn(true);

        assertThatThrownBy(() -> service.create(createRequest("Electronics")))
                .isInstanceOf(DuplicateSkuException.class);

        verify(productRepository, never()).saveAndFlush(any());
    }

    @Test
    void create_invalidCategory_throwsInvalidCategory() {
        given(categoryService.getByName("Toys"))
                .willThrow(new InvalidCategoryException("Toys", List.of("Electronics")));

        assertThatThrownBy(() -> service.create(createRequest("Toys")))
                .isInstanceOf(InvalidCategoryException.class);
    }

    @Test
    void create_raceOnUniqueSku_translatesToDuplicateSku() {
        given(categoryService.getByName("Electronics")).willReturn(electronics);
        given(productRepository.saveAndFlush(any(Product.class)))
                .willThrow(violation("duplicate key value violates unique constraint \"uq_products_sku\""));

        assertThatThrownBy(() -> service.create(createRequest("Electronics")))
                .isInstanceOf(DuplicateSkuException.class);
    }

    @Test
    void create_otherIntegrityViolation_isPropagated() {
        DataIntegrityViolationException other = violation("violates check constraint \"chk_products_price\"");
        given(categoryService.getByName("Electronics")).willReturn(electronics);
        given(productRepository.saveAndFlush(any(Product.class))).willThrow(other);

        assertThatThrownBy(() -> service.create(createRequest("Electronics"))).isSameAs(other);
    }

    @Test
    void update_byUuid_replacesDetailsAndReturnsResponse() {
        given(productRepository.findByIdAndActiveTrue(ID)).willReturn(Optional.of(existing));
        given(categoryService.getByName("electronics")).willReturn(electronics);
        given(productRepository.saveAndFlush(existing)).willReturn(existing);

        ProductResponse response = service.update(ID.toString(), updateRequest(null), Optional.empty());

        assertThat(response.name()).isEqualTo("Mouse Pro");
        assertThat(response.price()).isEqualByComparingTo("30.00");
        assertThat(response.stock()).isEqualTo(90);
        assertThat(response.description()).isNull();
        assertThat(response.weightKg()).isNull();
        assertThat(response.sku()).isEqualTo("ELECMS001");
    }

    @Test
    void update_bySku_resolvesProduct() {
        given(productRepository.findBySkuAndActiveTrue("ELECMS001")).willReturn(Optional.of(existing));
        given(categoryService.getByName("electronics")).willReturn(electronics);
        given(productRepository.saveAndFlush(existing)).willReturn(existing);

        ProductResponse response = service.update(" ELECMS001 ", updateRequest("ELECMS001"), Optional.empty());

        assertThat(response.id()).isEqualTo(ID);
    }

    @Test
    void update_unknownIdentifier_throwsNotFound() {
        given(productRepository.findBySkuAndActiveTrue("NOPE")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.update("NOPE", updateRequest(null), Optional.empty()))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void update_unknownUuid_fallsBackToSkuThenThrowsNotFound() {
        given(productRepository.findByIdAndActiveTrue(ID)).willReturn(Optional.empty());
        given(productRepository.findBySkuAndActiveTrue(ID.toString())).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(ID.toString(), updateRequest(null), Optional.empty()))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void update_differentSku_throwsImmutableSku() {
        given(productRepository.findByIdAndActiveTrue(ID)).willReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.update(ID.toString(), updateRequest("OTHER1"), Optional.empty()))
                .isInstanceOf(ImmutableSkuException.class);

        verify(productRepository, never()).saveAndFlush(any());
    }

    @Test
    void update_staleIfMatch_throwsOptimisticLockingFailure() {
        given(productRepository.findByIdAndActiveTrue(ID)).willReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.update(ID.toString(), updateRequest(null), Optional.of(2L)))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);

        verify(productRepository, never()).saveAndFlush(any());
    }

    @Test
    void update_matchingIfMatch_succeeds() {
        given(productRepository.findByIdAndActiveTrue(ID)).willReturn(Optional.of(existing));
        given(categoryService.getByName("electronics")).willReturn(electronics);
        given(productRepository.saveAndFlush(existing)).willReturn(existing);

        assertThat(service.update(ID.toString(), updateRequest(null), Optional.of(3L)).version()).isEqualTo(3L);
    }

    @Test
    void update_invalidCategory_throwsAndDoesNotSave() {
        given(productRepository.findByIdAndActiveTrue(ID)).willReturn(Optional.of(existing));
        given(categoryService.getByName("electronics"))
                .willThrow(new InvalidCategoryException("electronics", List.of("Books")));

        assertThatThrownBy(() -> service.update(ID.toString(), updateRequest(null), Optional.empty()))
                .isInstanceOf(InvalidCategoryException.class);

        verify(productRepository, never()).saveAndFlush(any());
    }

    @Test
    void delete_activeProduct_softDeletesWithClockInstantAndReturnsId() {
        given(productRepository.findByIdAndActiveTrue(ID)).willReturn(Optional.of(existing));

        UUID deleted = service.delete(ID.toString());

        assertThat(deleted).isEqualTo(ID);
        assertThat(existing.isActive()).isFalse();
        assertThat(existing.getDeletedAt()).isEqualTo(NOW);
        verify(productRepository).saveAndFlush(existing);
    }

    @Test
    void delete_bySku_returnsProductUuid() {
        given(productRepository.findBySkuAndActiveTrue("ELECMS001")).willReturn(Optional.of(existing));

        assertThat(service.delete("ELECMS001")).isEqualTo(ID);
    }

    @Test
    void delete_alreadyDeletedOrUnknown_throwsNotFound() {
        given(productRepository.findBySkuAndActiveTrue("ELECMS001")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete("ELECMS001")).isInstanceOf(ProductNotFoundException.class);

        verify(productRepository, never()).saveAndFlush(any());
    }
}
