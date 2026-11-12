package com.gila.api.ecommerce.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gila.api.ecommerce.model.dto.CustomPageRequest;
import com.gila.api.ecommerce.model.error.CustomErrorDetail;
import com.gila.api.ecommerce.model.error.CustomErrorResponse;
import com.gila.api.ecommerce.model.error.CustomRequestDetails;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;

class ErrorDateAndCommonUtilsTest {

    @Test
    void buildFieldErrorDetails_mixedErrors_keepsOnlyFieldErrorsSortedByField() {
        List<ObjectError> errors = List.of(
                new FieldError("p", "sku", "bad", false, null, null, "sku invalid"),
                new ObjectError("p", "global problem"),
                new FieldError("p", "name", null, false, null, null, "name required"));

        List<CustomErrorDetail> details = ErrorUtil.buildFieldErrorDetails(errors);

        assertThat(details).usingRecursiveComparison().isEqualTo(List.of(
                new CustomErrorDetail("name", "null", "name required"),
                new CustomErrorDetail("sku", "bad", "sku invalid")));
    }

    @Test
    void buildSimpleErrorDetail_nullException_usesGenericMessage() {
        assertThat(ErrorUtil.buildSimpleErrorDetail(null).message()).isEqualTo("An unexpected error occurred");
    }

    @Test
    void buildSimpleErrorDetail_exceptionWithoutMessage_usesGenericMessage() {
        assertThat(ErrorUtil.buildSimpleErrorDetail(new RuntimeException()).message())
                .isEqualTo("An unexpected error occurred");
    }

    @Test
    void buildSimpleErrorDetail_withExtraMessage_appendsIt() {
        CustomErrorDetail detail = ErrorUtil.buildSimpleErrorDetail(new RuntimeException("boom"), "extra");

        assertThat(detail.message()).isEqualTo("boom: extra");
        assertThat(detail.field()).isNull();
    }

    @Test
    void buildSimpleErrorDetail_withFieldAndValue_keepsThem() {
        CustomErrorDetail detail = ErrorUtil.buildSimpleErrorDetail("sku", "ab", new RuntimeException("boom"), "x");

        assertThat(detail).usingRecursiveComparison().isEqualTo(new CustomErrorDetail("sku", "ab", "boom: x"));
    }

    @Test
    void buildCustomErrorResponse_withRequest_includesPathMethodAndStatus() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/v1/products");

        CustomErrorResponse response = ErrorUtil.buildCustomErrorResponse(new IllegalStateException(), "detail",
                HttpStatus.CONFLICT, request, List.of());

        assertThat(response.error()).isEqualTo("IllegalStateException: detail");
        assertThat(response.response()).isEqualTo("Conflict");
        assertThat(response.statusCode()).isEqualTo(409);
        assertThat(response.requestDetails()).isEqualTo(new CustomRequestDetails("/v1/products", "POST"));
    }

    @Test
    void buildCustomErrorResponse_withoutRequestOrError_omitsDetailsAndUsesClassName() {
        CustomErrorResponse response = ErrorUtil.buildCustomErrorResponse(new IllegalStateException(), null,
                HttpStatus.BAD_REQUEST, null, List.of());

        assertThat(response.error()).isEqualTo("IllegalStateException");
        assertThat(response.requestDetails()).isNull();
    }

    @Test
    void parseErrorDate_usesIso8601WithMexicoCityOffset() {
        assertThat(DateUtil.parseErrorDate()).matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[+-]0[5-6]00");
    }

    @Test
    void buildPageable_explicitDirection_usesItWithTieBreaker() {
        Pageable pageable = CommonUtils.buildPageable(new CustomPageRequest(1, 20, null, Sort.Direction.DESC),
                "name", Sort.Direction.ASC, "id");

        assertThat(pageable.getPageNumber()).isEqualTo(1);
        assertThat(pageable.getPageSize()).isEqualTo(20);
        assertThat(pageable.getSort()).containsExactly(Sort.Order.desc("name"), Sort.Order.desc("id"));
    }

    @Test
    void buildPageable_noDirection_fallsBackToDefault() {
        Pageable pageable = CommonUtils.buildPageable(new CustomPageRequest(0, 10, null, null), "name",
                Sort.Direction.ASC, "id");

        assertThat(pageable.getSort()).containsExactly(Sort.Order.asc("name"), Sort.Order.asc("id"));
    }

    @Test
    void utilityClasses_cannotBeInstantiated() throws Exception {
        for (Class<?> type : List.of(ErrorUtil.class, DateUtil.class, CommonUtils.class)) {
            Constructor<?> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            assertThatThrownBy(constructor::newInstance).isInstanceOf(InvocationTargetException.class)
                    .hasCauseInstanceOf(UnsupportedOperationException.class);
        }
    }
}
