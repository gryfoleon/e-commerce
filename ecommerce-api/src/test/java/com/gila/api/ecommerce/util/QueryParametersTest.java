package com.gila.api.ecommerce.util;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gila.api.ecommerce.error.exceptions.UnsupportedQueryParameterException;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class QueryParametersTest {

    private static final Set<String> ALLOWED = Set.of("name", "sku");

    private static MockHttpServletRequest request(String... namesAndValues) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        for (int i = 0; i < namesAndValues.length; i += 2) {
            request.addParameter(namesAndValues[i], namesAndValues[i + 1]);
        }
        return request;
    }

    @Test
    void requireOnly_noParameters_passes() {
        assertThatCode(() -> QueryParameters.requireOnly(request(), ALLOWED)).doesNotThrowAnyException();
    }

    @Test
    void requireOnly_endpointAndPagingParameters_pass() {
        MockHttpServletRequest request = request("name", "mouse", "page", "0", "size", "20", "sortBy", "price",
                "sortDirection", "ASC");

        assertThatCode(() -> QueryParameters.requireOnly(request, ALLOWED)).doesNotThrowAnyException();
    }

    @Test
    void requireOnly_unknownParameter_throwsUnsupportedQueryParameter() {
        assertThatThrownBy(() -> QueryParameters.requireOnly(request("foo", "1"), ALLOWED))
                .isInstanceOf(UnsupportedQueryParameterException.class);
    }

    @Test
    void requireOnly_unknownParameterCombinedWithName_stillThrows() {
        assertThatThrownBy(() -> QueryParameters.requireOnly(request("name", "mouse", "foo", "1"), ALLOWED))
                .isInstanceOf(UnsupportedQueryParameterException.class);
    }

    @Test
    void requireOnly_parameterNamesAreCaseSensitive() {
        assertThatThrownBy(() -> QueryParameters.requireOnly(request("SortBy", "price"), ALLOWED))
                .isInstanceOf(UnsupportedQueryParameterException.class);
    }
}
