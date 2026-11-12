package com.gila.api.ecommerce.model.dto.product;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ProductRequestValidationTest {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    private static <T> Set<String> violatedFields(T target) {
        return VALIDATOR.validate(target).stream()
                .map(violation -> violation.getPropertyPath().toString()).collect(Collectors.toSet());
    }

    private static ProductCreateRequest create(String sku, BigDecimal price, Integer stock, BigDecimal weight) {
        return new ProductCreateRequest(sku, "Mouse", "desc", "Electronics", price, stock, weight);
    }

    private static ProductUpdateRequest update(String sku) {
        return new ProductUpdateRequest(sku, "Mouse", null, "Electronics", BigDecimal.TEN, 1, null);
    }

    private static ProductFilterParams filter(String name, String sku, BigDecimal minPrice, BigDecimal maxPrice,
                                              BigDecimal minWeight, BigDecimal maxWeight, Integer minStock) {
        return new ProductFilterParams(name, sku, null, minPrice, maxPrice, minStock, minWeight, maxWeight);
    }

    @Test
    void create_normalizesStripsAndBlankDescriptionBecomesNull() {
        ProductCreateRequest request = new ProductCreateRequest("  ELEC1 ", "  Mouse  ", "   ", " Electronics ",
                BigDecimal.TEN, 1, null);

        assertThat(request.sku()).isEqualTo("ELEC1");
        assertThat(request.name()).isEqualTo("Mouse");
        assertThat(request.description()).isNull();
        assertThat(request.category()).isEqualTo("Electronics");
    }

    @Test
    void create_validPayload_hasNoViolations() {
        assertThat(violatedFields(create("ELEC1", new BigDecimal("0.01"), 0, new BigDecimal("0.000")))).isEmpty();
        assertThat(violatedFields(create("ABCDEFGHI0", new BigDecimal("9999999999.99"), 5, null))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"rs001", "RS-001", "RS 001", "ABCDEFGHIJK", ""})
    void create_invalidSku_isRejected(String sku) {
        assertThat(violatedFields(create(sku, BigDecimal.TEN, 1, null))).contains("sku");
    }

    @Test
    void create_priceStockAndWeightRules_areEnforced() {
        assertThat(violatedFields(create("A1", BigDecimal.ZERO, 1, null))).containsExactly("price");
        assertThat(violatedFields(create("A1", new BigDecimal("1.001"), 1, null))).containsExactly("price");
        assertThat(violatedFields(create("A1", null, 1, null))).containsExactly("price");
        assertThat(violatedFields(create("A1", BigDecimal.TEN, -1, null))).containsExactly("stock");
        assertThat(violatedFields(create("A1", BigDecimal.TEN, null, null))).containsExactly("stock");
        assertThat(violatedFields(create("A1", BigDecimal.TEN, 1, new BigDecimal("-0.1")))).containsExactly("weightKg");
        assertThat(violatedFields(create("A1", BigDecimal.TEN, 1, new BigDecimal("0.1234")))).containsExactly("weightKg");
    }

    @Test
    void create_blankNameOrCategoryAndOversizedFields_areRejected() {
        ProductCreateRequest request = new ProductCreateRequest("A1", "  ", "d".repeat(5001), "", BigDecimal.TEN,
                1, null);

        assertThat(violatedFields(request)).containsExactlyInAnyOrder("name", "description", "category");
        assertThat(violatedFields(new ProductCreateRequest("A1", "n".repeat(256), null, "Electronics",
                BigDecimal.TEN, 1, null))).containsExactly("name");
    }

    @Test
    void update_blankSku_becomesNullAndOptionalSkuIsEmpty() {
        assertThat(update("   ").sku()).isNull();
        assertThat(update("   ").optionalSku()).isEmpty();
        assertThat(update(" ELEC1 ").optionalSku()).contains("ELEC1");
    }

    @Test
    void update_absentSku_isValid() {
        assertThat(violatedFields(update(null))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"elec1", "ELEC-1", "ABCDEFGHIJK"})
    void update_invalidSku_isRejected(String sku) {
        assertThat(violatedFields(update(sku))).contains("sku");
    }

    @Test
    void filter_normalization_blankNameAndCategoryBecomeNull() {
        ProductFilterParams params = new ProductFilterParams("   ", " MS0 ", "  ", null, null, null, null, null);

        assertThat(params.name()).isNull();
        assertThat(params.category()).isNull();
        assertThat(params.sku()).isEqualTo("MS0");
    }

    @Test
    void filter_allOptional_isValid() {
        assertThat(violatedFields(filter(null, null, null, null, null, null, null))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"MS", "ms0", "MS-0", "ABCDEFGHIJK"})
    void filter_invalidSkuFragment_isRejected(String sku) {
        assertThat(violatedFields(filter(null, sku, null, null, null, null, null))).containsExactly("sku");
    }

    @ParameterizedTest
    @ValueSource(strings = {"MS0", "ABCDEFGHIJ"})
    void filter_validSkuFragment_isAccepted(String sku) {
        assertThat(violatedFields(filter(null, sku, null, null, null, null, null))).isEmpty();
    }

    @Test
    void filter_nameLongerThan100_isRejected() {
        assertThat(violatedFields(filter("n".repeat(101), null, null, null, null, null, null)))
                .containsExactly("name");
        assertThat(violatedFields(filter("n".repeat(100), null, null, null, null, null, null))).isEmpty();
    }

    @Test
    void filter_invertedPriceRange_isRejected() {
        assertThat(violatedFields(filter(null, null, BigDecimal.TEN, BigDecimal.ONE, null, null, null)))
                .containsExactly("priceRangeValid");
        assertThat(violatedFields(filter(null, null, BigDecimal.TEN, BigDecimal.TEN, null, null, null))).isEmpty();
    }

    @Test
    void filter_invertedWeightRange_isRejected() {
        assertThat(violatedFields(filter(null, null, null, null, BigDecimal.TEN, BigDecimal.ONE, null)))
                .containsExactly("weightRangeValid");
    }

    @Test
    void filter_negativeBounds_areRejected() {
        ProductFilterParams params = filter(null, null, new BigDecimal("-1"), new BigDecimal("-1"),
                new BigDecimal("-1"), new BigDecimal("-1"), -1);

        assertThat(violatedFields(params)).containsExactlyInAnyOrder("minPrice", "maxPrice", "minWeightKg",
                "maxWeightKg", "minStock");
    }

    @Test
    void filter_zeroMinStock_isAllowed() {
        assertThat(violatedFields(filter(null, null, null, null, null, null, 0))).isEmpty();
    }
}
