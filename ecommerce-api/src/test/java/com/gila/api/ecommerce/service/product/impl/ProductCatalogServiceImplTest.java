package com.gila.api.ecommerce.service.product.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.gila.api.ecommerce.error.exceptions.InvalidCategoryException;
import com.gila.api.ecommerce.model.dto.PageableResponse;
import com.gila.api.ecommerce.model.dto.product.ProductFilterParams;
import com.gila.api.ecommerce.model.dto.product.ProductSearchDTO;
import com.gila.api.ecommerce.model.query.ProductFilterCriteria;
import com.gila.api.ecommerce.service.category.CategoryService;
import com.gila.api.ecommerce.service.product.ProductQueryService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class ProductCatalogServiceImplTest {

    private final Pageable pageable = PageRequest.of(0, 20);
    private final PageableResponse<ProductSearchDTO> page =
            new PageableResponse<>(List.of(), new PageableResponse.PageDetails(0, 20, 0, 0L, true));

    @Mock
    private ProductQueryService queryService;
    @Mock
    private CategoryService categoryService;
    @InjectMocks
    private ProductCatalogServiceImpl service;

    @Test
    void filter_allCriteria_areForwardedWithCanonicalCategory() {
        given(categoryService.resolveCanonicalName("electronics")).willReturn("Electronics");
        given(queryService.findPage(any(), any())).willReturn(page);
        ProductFilterParams params = new ProductFilterParams("mouse", "MS0", "electronics", new BigDecimal("10"),
                new BigDecimal("150"), 1, new BigDecimal("0.1"), new BigDecimal("2"));

        PageableResponse<ProductSearchDTO> result = service.filter(params, pageable);

        ArgumentCaptor<ProductFilterCriteria> criteria = ArgumentCaptor.forClass(ProductFilterCriteria.class);
        verify(queryService).findPage(criteria.capture(), org.mockito.ArgumentMatchers.eq(pageable));
        assertThat(criteria.getValue()).isEqualTo(new ProductFilterCriteria("mouse", "MS0", "Electronics",
                new BigDecimal("10"), new BigDecimal("150"), 1, new BigDecimal("0.1"), new BigDecimal("2")));
        assertThat(result).isSameAs(page);
    }

    @Test
    void filter_withoutCategory_doesNotResolveCategory() {
        given(queryService.findPage(any(), any())).willReturn(page);

        service.filter(new ProductFilterParams(null, null, null, null, null, null, null, null), pageable);

        verify(categoryService, never()).resolveCanonicalName(any());
    }

    @Test
    void filter_blankNameAndCategory_areTreatedAsAbsent() {
        given(queryService.findPage(any(), any())).willReturn(page);

        service.filter(new ProductFilterParams("   ", null, "  ", null, null, null, null, null), pageable);

        ArgumentCaptor<ProductFilterCriteria> criteria = ArgumentCaptor.forClass(ProductFilterCriteria.class);
        verify(queryService).findPage(criteria.capture(), any());
        assertThat(criteria.getValue().name()).isNull();
        assertThat(criteria.getValue().category()).isNull();
        verify(categoryService, never()).resolveCanonicalName(any());
    }

    @Test
    void filter_unknownCategory_propagatesInvalidCategory() {
        given(categoryService.resolveCanonicalName("toys"))
                .willThrow(new InvalidCategoryException("toys", List.of("Electronics")));

        assertThatThrownBy(() -> service.filter(
                new ProductFilterParams(null, null, "toys", null, null, null, null, null), pageable))
                .isInstanceOf(InvalidCategoryException.class);

        verify(queryService, never()).findPage(any(), any());
    }
}
