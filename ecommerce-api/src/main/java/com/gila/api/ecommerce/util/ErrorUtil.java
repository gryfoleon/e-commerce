package com.gila.api.ecommerce.util;

import com.gila.api.ecommerce.configuration.constants.GlobalConstants;
import com.gila.api.ecommerce.model.error.CustomErrorDetail;
import com.gila.api.ecommerce.model.error.CustomErrorResponse;
import com.gila.api.ecommerce.model.error.CustomRequestDetails;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;

@Slf4j
public final class ErrorUtil {

    private static final Comparator<CustomErrorDetail> BY_FIELD =
            Comparator.comparing(CustomErrorDetail::field, Comparator.nullsFirst(Comparator.naturalOrder()));

    private ErrorUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static List<CustomErrorDetail> buildFieldErrorDetails(List<? extends ObjectError> errors) {
        errors.stream()
                .filter(error -> !(error instanceof FieldError))
                .forEach(globalError -> log.debug(
                        "Global validation error: {} = {}",
                        globalError.getObjectName(),
                        globalError.getDefaultMessage()));

        return errors.stream()
                .filter(FieldError.class::isInstance)
                .map(FieldError.class::cast)
                .map(fieldError -> new CustomErrorDetail(
                        fieldError.getField(),
                        String.valueOf(fieldError.getRejectedValue()),
                        fieldError.getDefaultMessage()))
                .sorted(BY_FIELD)
                .toList();
    }

    public static CustomErrorDetail buildSimpleErrorDetail(@Nullable Exception ex) {
        return new CustomErrorDetail(null, null, resolveMessage(ex));
    }

    public static CustomErrorDetail buildSimpleErrorDetail(@Nullable Exception ex, String extraMessage) {
        return new CustomErrorDetail(null, null, resolveMessage(ex) + ": " + extraMessage);
    }

    public static CustomErrorDetail buildSimpleErrorDetail(
            @Nullable String field,
            @Nullable Object rejectedValue,
            @Nullable Exception ex,
            String extraMessage) {
        return new CustomErrorDetail(field, (String) rejectedValue, resolveMessage(ex) + ": " + extraMessage);
    }

    public static CustomErrorResponse buildCustomErrorResponse(
            Throwable exception,
            @Nullable String error,
            HttpStatus httpStatus,
            @Nullable HttpServletRequest request,
            List<CustomErrorDetail> errorDetails) {
        CustomRequestDetails customRequestDetails = request != null
                ? new CustomRequestDetails(request.getRequestURI(), request.getMethod())
                : null;

        String errorString = error != null
                ? exception.getClass().getSimpleName() + ": " + error
                : exception.getClass().getSimpleName();

        return new CustomErrorResponse(
                errorString,
                httpStatus.getReasonPhrase(),
                httpStatus.value(),
                DateUtil.parseErrorDate(),
                customRequestDetails,
                errorDetails);
    }

    private static String resolveMessage(@Nullable Exception ex) {
        return ex != null ? Objects.requireNonNullElse(ex.getMessage(), GlobalConstants.GENERAL_ERROR)
                : GlobalConstants.GENERAL_ERROR;
    }
}
