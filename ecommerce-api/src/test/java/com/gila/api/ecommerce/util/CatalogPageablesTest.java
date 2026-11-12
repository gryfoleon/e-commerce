package com.gila.api.ecommerce.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gila.api.ecommerce.error.exceptions.InvalidSortException;
import com.gila.api.ecommerce.model.dto.CustomPageRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;

class CatalogPageablesTest {

    private static CustomPageRequest request(String sortBy) {
        return new CustomPageRequest(null, null, sortBy, null);
    }

    @Test
    void forFilter_noSortBy_defaultsToCreatedAtDescWithIdTieBreaker() {
        Pageable pageable = CatalogPageables.forFilter(request(null));

        assertThat(pageable.getSort().stream().map(order -> order.getProperty() + ":" + order.getDirection()))
                .containsExactly("createdAt:DESC", "id:DESC");
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(20);
    }

    @Test
    void forFilter_priceAndNameSorts_areAllowed() {
        assertThat(CatalogPageables.forFilter(request("price")).getSort().getOrderFor("price")).isNotNull();
        assertThat(CatalogPageables.forFilter(request("name")).getSort().getOrderFor("name")).isNotNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"relevance", "unknown", "id", "sku"})
    void forFilter_notWhitelistedSort_throwsInvalidSort(String sortBy) {
        assertThatThrownBy(() -> CatalogPageables.forFilter(request(sortBy)))
                .isInstanceOf(InvalidSortException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"relevance", "unknown"})
    void forList_notWhitelistedSort_throwsInvalidSort(String sortBy) {
        assertThatThrownBy(() -> CatalogPageables.forList(request(sortBy)))
                .isInstanceOf(InvalidSortException.class);
    }

    @ParameterizedTest
    @CsvSource({"createdAt,createdAt,DESC", "CREATEDAT,createdAt,DESC", "price,price,ASC", "PRICE,price,ASC",
            "Name,name,ASC"})
    void forList_sortByIsCaseInsensitive_andUsesDefaultDirectionPerField(
            String sortBy, String property, Direction expected) {
        Sort sort = CatalogPageables.forList(request(sortBy)).getSort();

        assertThat(sort.getOrderFor(property).getDirection()).isEqualTo(expected);
        assertThat(sort.getOrderFor("id").getDirection()).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({"createdAt,ASC", "price,DESC", "name,DESC"})
    void forList_explicitDirection_overridesDefaultAndAppliesToTieBreaker(String sortBy, Direction direction) {
        Sort sort = CatalogPageables.forList(new CustomPageRequest(2, 50, sortBy, direction)).getSort();

        assertThat(sort.stream()).hasSize(2).allSatisfy(order -> assertThat(order.getDirection()).isEqualTo(direction));
        assertThat(sort.stream().map(Sort.Order::getProperty)).containsExactly(sortBy, "id");
    }

    @Test
    void forList_pageAndSize_areCarriedToThePageable() {
        Pageable pageable = CatalogPageables.forList(new CustomPageRequest(3, 7, null, null));

        assertThat(pageable.getPageNumber()).isEqualTo(3);
        assertThat(pageable.getPageSize()).isEqualTo(7);
    }

    @Test
    void forFilter_sameRulesAsForList() {
        CustomPageRequest request = new CustomPageRequest(1, 10, "price", Direction.DESC);

        assertThat(CatalogPageables.forFilter(request)).isEqualTo(CatalogPageables.forList(request));
    }
}
