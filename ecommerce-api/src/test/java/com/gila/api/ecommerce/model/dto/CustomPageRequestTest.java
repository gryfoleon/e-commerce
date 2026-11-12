package com.gila.api.ecommerce.model.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.data.domain.Sort;

class CustomPageRequestTest {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    private static Set<String> violatedFields(CustomPageRequest request) {
        return VALIDATOR.validate(request).stream()
                .map(violation -> violation.getPropertyPath().toString()).collect(Collectors.toSet());
    }

    @Test
    void constructor_nullValues_appliesDefaults() {
        CustomPageRequest request = new CustomPageRequest(null, null, null, null);

        assertThat(request.page()).isZero();
        assertThat(request.size()).isEqualTo(20);
        assertThat(request.sortBy()).isNull();
        assertThat(request.sortDirection()).isNull();
    }

    @ParameterizedTest
    @CsvSource(value = {"'',NULL", "'   ',NULL", "' price ',price"}, nullValues = "NULL")
    void constructor_sortBy_blankBecomesNullAndValueIsStripped(String raw, String expected) {
        assertThat(new CustomPageRequest(0, 20, raw, null).sortBy()).isEqualTo(expected);
    }

    @Test
    void validation_valuesWithinLimits_isValid() {
        assertThat(violatedFields(new CustomPageRequest(0, 1, null, Sort.Direction.ASC))).isEmpty();
        assertThat(violatedFields(new CustomPageRequest(1000, 100, "price", null))).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({"-1,20,page", "1001,20,page", "0,0,size", "0,-5,size", "0,101,size"})
    void validation_outOfRange_reportsTheOffendingField(int page, int size, String field) {
        assertThat(violatedFields(new CustomPageRequest(page, size, null, null))).containsExactly(field);
    }

    @Test
    void validation_sortByLongerThan50_isRejected() {
        assertThat(violatedFields(new CustomPageRequest(0, 20, "x".repeat(51), null))).containsExactly("sortBy");
    }
}
