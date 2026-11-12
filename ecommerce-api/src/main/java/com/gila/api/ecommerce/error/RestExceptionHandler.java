package com.gila.api.ecommerce.error;

import com.gila.api.ecommerce.configuration.constants.GlobalConstants;
import com.gila.api.ecommerce.error.exceptions.CategoryNotFoundException;
import com.gila.api.ecommerce.error.exceptions.ConcurrentIdempotencyException;
import com.gila.api.ecommerce.error.exceptions.CsvIngestionRejectedException;
import com.gila.api.ecommerce.error.exceptions.DuplicateCategoryException;
import com.gila.api.ecommerce.error.exceptions.DuplicateSkuException;
import com.gila.api.ecommerce.error.exceptions.IdempotencyKeyMismatchException;
import com.gila.api.ecommerce.error.exceptions.ImmutableSkuException;
import com.gila.api.ecommerce.error.exceptions.InsufficientPermissionsException;
import com.gila.api.ecommerce.error.exceptions.InternalSecurityException;
import com.gila.api.ecommerce.error.exceptions.InvalidCategoryException;
import com.gila.api.ecommerce.error.exceptions.InvalidCredentialsException;
import com.gila.api.ecommerce.error.exceptions.InvalidCsvFileException;
import com.gila.api.ecommerce.error.exceptions.InvalidSortException;
import com.gila.api.ecommerce.error.exceptions.InvalidIdempotencyKeyException;
import com.gila.api.ecommerce.error.exceptions.InvalidPreconditionException;
import com.gila.api.ecommerce.error.exceptions.MissingIdempotencyKeyException;
import com.gila.api.ecommerce.error.exceptions.ProductNotFoundException;
import com.gila.api.ecommerce.error.exceptions.UnsupportedQueryParameterException;
import com.gila.api.ecommerce.model.dto.product.CsvRowError;
import com.gila.api.ecommerce.model.error.CustomErrorDetail;
import com.gila.api.ecommerce.model.error.CustomErrorResponse;
import com.gila.api.ecommerce.util.ErrorUtil;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletionException;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;

@Slf4j
@RestControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public CustomErrorResponse handleNoHandlerFoundException(
            NoHandlerFoundException exception, HttpServletRequest request) {
        log.error("No handler found for {} {}: {}", request.getMethod(), request.getRequestURI(), exception.getMessage());
        CustomErrorDetail customErrorDetail = ErrorUtil.buildSimpleErrorDetail(
                exception,
                ". There might be a typo in the URL, there might be missing parameters"
                        + " or the HTTP method used is not supported.");
        return ErrorUtil.buildCustomErrorResponse(
                exception, GlobalConstants.GENERAL_ERROR, HttpStatus.NOT_FOUND, request, List.of(customErrorDetail));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public CustomErrorResponse handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException exception, HttpServletRequest request) {
        log.error("HTTP method not supported for {} {}: {}",
                request.getMethod(), request.getRequestURI(), exception.getMessage());
        CustomErrorDetail customErrorDetail = ErrorUtil.buildSimpleErrorDetail(
                exception, ". The HTTP method used is not supported for this URL.");
        return ErrorUtil.buildCustomErrorResponse(
                exception, GlobalConstants.GENERAL_ERROR, HttpStatus.METHOD_NOT_ALLOWED, request,
                List.of(customErrorDetail));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CustomErrorResponse handleValidationExceptions(
            MethodArgumentNotValidException exception, HttpServletRequest request) {
        log.error("Validation error for {} {}: {}", request.getMethod(), request.getRequestURI(), exception.getMessage());
        List<ObjectError> validationErrors = exception.getBindingResult().getAllErrors();
        List<CustomErrorDetail> fieldErrorsDetail = validationErrors.isEmpty()
                ? List.of()
                : ErrorUtil.buildFieldErrorDetails(validationErrors);
        return ErrorUtil.buildCustomErrorResponse(
                exception, GlobalConstants.VALIDATION_ERROR, HttpStatus.BAD_REQUEST, request, fieldErrorsDetail);
    }

    @ExceptionHandler(CompletionException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public CustomErrorResponse handleCompletionException(CompletionException exception, HttpServletRequest request) {
        log.error("Completion exception for {} {}: {}",
                request.getMethod(), request.getRequestURI(), exception.getMessage());
        return ErrorUtil.buildCustomErrorResponse(
                exception, GlobalConstants.GENERAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, request,
                List.of(ErrorUtil.buildSimpleErrorDetail(exception)));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CustomErrorResponse handleIllegalArgumentException(
            IllegalArgumentException exception, HttpServletRequest request) {
        log.error("Illegal argument for {} {}: {}", request.getMethod(), request.getRequestURI(), exception.getMessage());
        return ErrorUtil.buildCustomErrorResponse(
                exception, GlobalConstants.VALIDATION_ERROR, HttpStatus.BAD_REQUEST, request,
                List.of(ErrorUtil.buildSimpleErrorDetail(exception)));
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public CustomErrorResponse handleOptimisticLockingFailureException(
            ObjectOptimisticLockingFailureException exception, HttpServletRequest request) {
        log.warn("Optimistic locking conflict for {} {}: {}",
                request.getMethod(), request.getRequestURI(), exception.getMessage());
        CustomErrorDetail customErrorDetail = ErrorUtil.buildSimpleErrorDetail(
                exception,
                ". Fetch the latest resource and retry the request with the updated If-Match header value.");
        return ErrorUtil.buildCustomErrorResponse(
                exception, GlobalConstants.VALIDATION_ERROR, HttpStatus.CONFLICT, request, List.of(customErrorDetail));
    }

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CustomErrorResponse handleNoSuchElementException(
            NoSuchElementException exception, HttpServletRequest request) {
        log.error("No such element for {} {}: {}", request.getMethod(), request.getRequestURI(), exception.getMessage());
        return ErrorUtil.buildCustomErrorResponse(
                exception, GlobalConstants.VALIDATION_ERROR, HttpStatus.BAD_REQUEST, request,
                List.of(ErrorUtil.buildSimpleErrorDetail(exception)));
    }

    @ExceptionHandler(InternalSecurityException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public CustomErrorResponse handleInternalSecurityException(
            InternalSecurityException exception, HttpServletRequest request) {
        log.error("Internal Security error for {} {}, message='{}'",
                request.getMethod(), request.getRequestURI(), exception.getMessage());
        return ErrorUtil.buildCustomErrorResponse(
                exception, GlobalConstants.AUTHENTICATION_ERROR, HttpStatus.UNAUTHORIZED, request,
                List.of(ErrorUtil.buildSimpleErrorDetail(exception)));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public CustomErrorResponse handleBadCredentialsException(
            InvalidCredentialsException exception, HttpServletRequest request) {
        log.error("Invalid credentials for {} {}, message='{}'",
                request.getMethod(), request.getRequestURI(), exception.getMessage());
        CustomErrorDetail customErrorDetail = ErrorUtil.buildSimpleErrorDetail(
                exception.getIdentifier(),
                exception.getRejectedValue(),
                exception,
                "Invalid credentials provided");
        return ErrorUtil.buildCustomErrorResponse(
                exception, GlobalConstants.BAD_CREDENTIALS, HttpStatus.UNAUTHORIZED, request,
                List.of(customErrorDetail));
    }

    @ExceptionHandler(InsufficientPermissionsException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public CustomErrorResponse handleInsufficientPermissionsException(
            InsufficientPermissionsException exception, HttpServletRequest request) {
        log.error("Insufficient permissions error for {} {}, message='{}'",
                request.getMethod(), request.getRequestURI(), exception.getMessage());
        return ErrorUtil.buildCustomErrorResponse(
                exception, GlobalConstants.AUTHENTICATION_ERROR, HttpStatus.FORBIDDEN, request,
                List.of(ErrorUtil.buildSimpleErrorDetail(exception)));
    }

    @ExceptionHandler(MissingIdempotencyKeyException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CustomErrorResponse handleMissingIdempotencyKeyException(
            MissingIdempotencyKeyException exception, HttpServletRequest request) {
        log.error("Missing idempotency key error for {} {}, message='{}'",
                request.getMethod(), request.getRequestURI(), exception.getMessage());
        return ErrorUtil.buildCustomErrorResponse(
                exception, GlobalConstants.GENERAL_ERROR, HttpStatus.BAD_REQUEST, request,
                List.of(ErrorUtil.buildSimpleErrorDetail(exception)));
    }

    @ExceptionHandler(ConcurrentIdempotencyException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public CustomErrorResponse handleConcurrentIdempotencyException(
            ConcurrentIdempotencyException exception, HttpServletRequest request) {
        log.error("Concurrent idempotency error for {} {}, message='{}'",
                request.getMethod(), request.getRequestURI(), exception.getMessage());
        return ErrorUtil.buildCustomErrorResponse(
                exception, GlobalConstants.GENERAL_ERROR, HttpStatus.CONFLICT, request,
                List.of(ErrorUtil.buildSimpleErrorDetail(exception)));
    }

    @ExceptionHandler(ProductNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public CustomErrorResponse handleProductNotFoundException(
            ProductNotFoundException exception, HttpServletRequest request) {
        log.warn("Product not found for {} {}: {}", request.getMethod(), request.getRequestURI(), exception.getMessage());
        return ErrorUtil.buildCustomErrorResponse(
                exception, GlobalConstants.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, request,
                List.of(new CustomErrorDetail("id", exception.getIdentifier(), exception.getMessage())));
    }

    @ExceptionHandler(CategoryNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public CustomErrorResponse handleCategoryNotFoundException(
            CategoryNotFoundException exception, HttpServletRequest request) {
        log.warn("Category not found for {} {}: {}", request.getMethod(), request.getRequestURI(), exception.getMessage());
        return ErrorUtil.buildCustomErrorResponse(
                exception, GlobalConstants.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND, request,
                List.of(new CustomErrorDetail("category", exception.getIdentifier(), exception.getMessage())));
    }

    @ExceptionHandler(DuplicateCategoryException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public CustomErrorResponse handleDuplicateCategoryException(
            DuplicateCategoryException exception, HttpServletRequest request) {
        log.warn("Duplicate category for {} {}: {}", request.getMethod(), request.getRequestURI(), exception.getMessage());
        return ErrorUtil.buildCustomErrorResponse(
                exception, GlobalConstants.RESOURCE_CONFLICT, HttpStatus.CONFLICT, request,
                List.of(new CustomErrorDetail(
                        DuplicateCategoryException.FIELD, exception.getName(), exception.getMessage())));
    }

    @ExceptionHandler(DuplicateSkuException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public CustomErrorResponse handleDuplicateSkuException(
            DuplicateSkuException exception, HttpServletRequest request) {
        log.warn("Duplicate SKU for {} {}: {}", request.getMethod(), request.getRequestURI(), exception.getMessage());
        return ErrorUtil.buildCustomErrorResponse(
                exception, GlobalConstants.RESOURCE_CONFLICT, HttpStatus.CONFLICT, request,
                List.of(new CustomErrorDetail(DuplicateSkuException.FIELD, exception.getSku(), exception.getMessage())));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public CustomErrorResponse handleDataIntegrityViolationException(
            DataIntegrityViolationException exception, HttpServletRequest request) {
        log.error("Data integrity violation for {} {}: {}",
                request.getMethod(), request.getRequestURI(), exception.getMostSpecificCause().getMessage());
        return ErrorUtil.buildCustomErrorResponse(
                exception, GlobalConstants.RESOURCE_CONFLICT, HttpStatus.CONFLICT, request,
                List.of(new CustomErrorDetail(null, null,
                        "The request conflicts with the current state of the resource or violates a data constraint.")));
    }

    @ExceptionHandler(InvalidCategoryException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CustomErrorResponse handleInvalidCategoryException(
            InvalidCategoryException exception, HttpServletRequest request) {
        log.warn("Invalid category for {} {}: {}", request.getMethod(), request.getRequestURI(), exception.getCategory());
        return ErrorUtil.buildCustomErrorResponse(
                exception, GlobalConstants.VALIDATION_ERROR, HttpStatus.BAD_REQUEST, request,
                List.of(new CustomErrorDetail(
                        InvalidCategoryException.FIELD, exception.getCategory(), exception.getMessage())));
    }

    @ExceptionHandler(ImmutableSkuException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CustomErrorResponse handleImmutableSkuException(
            ImmutableSkuException exception, HttpServletRequest request) {
        log.warn("Immutable SKU violation for {} {}: {}",
                request.getMethod(), request.getRequestURI(), exception.getMessage());
        return ErrorUtil.buildCustomErrorResponse(
                exception, GlobalConstants.VALIDATION_ERROR, HttpStatus.BAD_REQUEST, request,
                List.of(new CustomErrorDetail(
                        ImmutableSkuException.FIELD, exception.getRequestedSku(), exception.getMessage())));
    }

    @ExceptionHandler(InvalidSortException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CustomErrorResponse handleInvalidSortException(
            InvalidSortException exception, HttpServletRequest request) {
        log.warn("Invalid sort for {} {}: {}", request.getMethod(), request.getRequestURI(), exception.getMessage());
        return ErrorUtil.buildCustomErrorResponse(
                exception, GlobalConstants.VALIDATION_ERROR, HttpStatus.BAD_REQUEST, request,
                List.of(new CustomErrorDetail(InvalidSortException.FIELD, exception.getSortBy(), exception.getMessage())));
    }

    @ExceptionHandler(UnsupportedQueryParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CustomErrorResponse handleUnsupportedQueryParameter(
            UnsupportedQueryParameterException exception, HttpServletRequest request) {
        log.warn("Unsupported query parameter for {} {}: {}",
                request.getMethod(), request.getRequestURI(), exception.getMessage());
        return ErrorUtil.buildCustomErrorResponse(
                exception, GlobalConstants.VALIDATION_ERROR, HttpStatus.BAD_REQUEST, request,
                List.of(new CustomErrorDetail(UnsupportedQueryParameterException.FIELD, exception.getParameter(),
                        exception.getMessage())));
    }

    @ExceptionHandler({InvalidIdempotencyKeyException.class, InvalidPreconditionException.class,
            MissingRequestHeaderException.class, MissingServletRequestParameterException.class,
            MissingServletRequestPartException.class, MethodArgumentTypeMismatchException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CustomErrorResponse handleBadRequestInputException(Exception exception, HttpServletRequest request) {
        log.warn("Invalid request input for {} {}: {}",
                request.getMethod(), request.getRequestURI(), exception.getMessage());
        return ErrorUtil.buildCustomErrorResponse(
                exception, GlobalConstants.VALIDATION_ERROR, HttpStatus.BAD_REQUEST, request,
                List.of(ErrorUtil.buildSimpleErrorDetail(exception)));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CustomErrorResponse handleHandlerMethodValidationException(
            HandlerMethodValidationException exception, HttpServletRequest request) {
        log.warn("Method validation error for {} {}: {}",
                request.getMethod(), request.getRequestURI(), exception.getMessage());
        List<CustomErrorDetail> details = exception.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> new CustomErrorDetail(
                                result.getMethodParameter().getParameterName(),
                                result.getArgument() != null ? String.valueOf(result.getArgument()) : null,
                                error.getDefaultMessage())))
                .toList();
        return ErrorUtil.buildCustomErrorResponse(
                exception, GlobalConstants.VALIDATION_ERROR, HttpStatus.BAD_REQUEST, request, details);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CustomErrorResponse handleHttpMessageNotReadableException(
            HttpMessageNotReadableException exception, HttpServletRequest request) {
        log.warn("Unreadable request body for {} {}: {}",
                request.getMethod(), request.getRequestURI(), exception.getMessage());
        return ErrorUtil.buildCustomErrorResponse(
                exception, GlobalConstants.VALIDATION_ERROR, HttpStatus.BAD_REQUEST, request,
                List.of(new CustomErrorDetail(null, null,
                        "The request body is missing or is not valid JSON for the expected contract.")));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    public CustomErrorResponse handleHttpMediaTypeNotSupportedException(
            HttpMediaTypeNotSupportedException exception, HttpServletRequest request) {
        log.warn("Unsupported media type for {} {}: {}",
                request.getMethod(), request.getRequestURI(), exception.getMessage());
        return ErrorUtil.buildCustomErrorResponse(
                exception, GlobalConstants.VALIDATION_ERROR, HttpStatus.UNSUPPORTED_MEDIA_TYPE, request,
                List.of(ErrorUtil.buildSimpleErrorDetail(exception)));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.CONTENT_TOO_LARGE)
    public CustomErrorResponse handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException exception, HttpServletRequest request) {
        log.warn("Upload too large for {} {}: {}", request.getMethod(), request.getRequestURI(), exception.getMessage());
        return ErrorUtil.buildCustomErrorResponse(
                exception, GlobalConstants.INGESTION_ERROR, HttpStatus.CONTENT_TOO_LARGE, request,
                List.of(new CustomErrorDetail("file", null, "The uploaded file exceeds the maximum allowed size.")));
    }

    @ExceptionHandler({InvalidCsvFileException.class, MultipartException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CustomErrorResponse handleInvalidCsvFileException(Exception exception, HttpServletRequest request) {
        log.warn("Invalid CSV upload for {} {}: {}", request.getMethod(), request.getRequestURI(), exception.getMessage());
        return ErrorUtil.buildCustomErrorResponse(
                exception, GlobalConstants.INGESTION_ERROR, HttpStatus.BAD_REQUEST, request,
                List.of(new CustomErrorDetail("file", null, exception.getMessage())));
    }

    @ExceptionHandler(CsvIngestionRejectedException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_CONTENT)
    public CustomErrorResponse handleCsvIngestionRejectedException(
            CsvIngestionRejectedException exception, HttpServletRequest request) {
        log.warn("CSV ingestion rejected for {} {}: {}",
                request.getMethod(), request.getRequestURI(), exception.getMessage());
        List<CustomErrorDetail> details = exception.getReport().errors().stream()
                .map(RestExceptionHandler::toErrorDetail)
                .toList();
        return ErrorUtil.buildCustomErrorResponse(
                exception, GlobalConstants.INGESTION_ERROR, HttpStatus.UNPROCESSABLE_CONTENT, request, details);
    }

    @ExceptionHandler(IdempotencyKeyMismatchException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_CONTENT)
    public CustomErrorResponse handleIdempotencyKeyMismatchException(
            IdempotencyKeyMismatchException exception, HttpServletRequest request) {
        log.warn("Idempotency key reused with a different payload for {} {}: {}",
                request.getMethod(), request.getRequestURI(), exception.getIdempotencyKey());
        return ErrorUtil.buildCustomErrorResponse(
                exception, GlobalConstants.IDEMPOTENCY_ERROR, HttpStatus.UNPROCESSABLE_CONTENT, request,
                List.of(new CustomErrorDetail(
                        GlobalConstants.HEADER_IDEMPOTENCY_KEY, exception.getIdempotencyKey(), exception.getMessage())));
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public CustomErrorResponse handleException(Exception exception, HttpServletRequest request) {
        log.error("Unhandled exception for {} {}: {}",
                request.getMethod(), request.getRequestURI(), exception.getMessage(), exception);
        return ErrorUtil.buildCustomErrorResponse(
                exception, GlobalConstants.GENERAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, request,
                List.of(ErrorUtil.buildSimpleErrorDetail(exception)));
    }

    private static CustomErrorDetail toErrorDetail(CsvRowError error) {
        String field = "row[" + error.line() + "]" + (error.field() != null ? "." + error.field() : "");
        return new CustomErrorDetail(field, error.rejectedValue(), error.message());
    }
}
