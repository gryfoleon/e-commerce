package com.gila.api.ecommerce.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.gila.api.ecommerce.configuration.properties.IngestionProperties;
import com.gila.api.ecommerce.model.cache.IdempotencyExecutionResult;
import com.gila.api.ecommerce.model.dto.product.CsvImportReport;
import com.gila.api.ecommerce.model.dto.product.CsvUploadMetadata;
import com.gila.api.ecommerce.model.dto.product.ProductCreateRequest;
import com.gila.api.ecommerce.model.dto.product.ProductResponse;
import com.gila.api.ecommerce.model.dto.product.ProductUpdateRequest;
import com.gila.api.ecommerce.service.cache.IdempotencyService;
import com.gila.api.ecommerce.service.product.ProductCatalogService;
import com.gila.api.ecommerce.service.product.ProductImportService;
import com.gila.api.ecommerce.service.product.ProductService;
import com.gila.api.ecommerce.util.HashUtil;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.request.ServletWebRequest;
import tools.jackson.databind.json.JsonMapper;

class ProductControllerTest {

    private static final UUID ID = UUID.fromString("3f1c1a52-5d0e-4bb8-9a7a-0f6f4f3b9c11");
    private static final String KEY = "0b0e8a5e-6a5c-4f27-9f2b-2d7d5b9f3a11";

    private final ProductService productService = mock(ProductService.class);
    private final ProductImportService importService = mock(ProductImportService.class);
    private final IdempotencyService idempotencyService = mock(IdempotencyService.class);
    private final IngestionProperties ingestion = new IngestionProperties(null, null, null, null);
    private ProductController controller;

    private final ProductResponse product = new ProductResponse(ID, "ELECMS001", "Mouse", null, "Electronics",
            new BigDecimal("24.99"), 10, null, 3L, Instant.parse("2025-01-01T00:00:00Z"),
            Instant.parse("2025-01-02T00:00:00Z"));
    private final CsvImportReport report = new CsvImportReport(2, 2, 0, 0, false, false, 5, List.of());

    @BeforeEach
    void setUp() {
        controller = new ProductController(productService, mock(ProductCatalogService.class), importService,
                idempotencyService, ingestion, JsonMapper.builder().build());
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/v1/products");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    private ProductCreateRequest createRequest() {
        return new ProductCreateRequest("ELECMS001", "Mouse", null, "Electronics", new BigDecimal("24.99"), 10, null);
    }

    @SuppressWarnings("unchecked")
    private void idempotencyRunsAction(boolean replay) {
        when(idempotencyService.execute(anyString(), anyString(), anyString(), any(Class.class), any(Supplier.class)))
                .thenAnswer(invocation -> new IdempotencyExecutionResult<>(
                        ((Supplier<Object>) invocation.getArgument(4)).get(), replay));
    }

    @Test
    void createProduct_validRequest_returns201WithLocationAndETag() {
        when(productService.create(any())).thenReturn(product);
        idempotencyRunsAction(false);

        ResponseEntity<ProductResponse> response = controller.createProduct(KEY, createRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getHeaders().getLocation()).hasToString("http://localhost/v1/products/" + ID);
        assertThat(response.getHeaders().getETag()).isEqualTo("\"3\"");
        assertThat(response.getHeaders().getFirst("Idempotent-Replayed")).isNull();
        assertThat(response.getBody()).isEqualTo(product);
    }

    @Test
    void createProduct_replayedKey_addsReplayedHeaderAndKeepsStatus() {
        idempotencyRunsAction(true);
        when(productService.create(any())).thenReturn(product);

        ResponseEntity<ProductResponse> response = controller.createProduct(KEY, createRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getHeaders().getFirst("Idempotent-Replayed")).isEqualTo("true");
    }

    @Test
    void createProduct_fingerprintIsDeterministicForTheSamePayload() {
        when(productService.create(any())).thenReturn(product);
        idempotencyRunsAction(false);
        ArgumentCaptor<String> fingerprint = ArgumentCaptor.forClass(String.class);

        controller.createProduct(KEY, createRequest());
        controller.createProduct(KEY, createRequest());

        verify(idempotencyService, org.mockito.Mockito.times(2)).execute(eq("product-create"), eq(KEY),
                fingerprint.capture(), eq(ProductResponse.class), any(Supplier.class));
        assertThat(fingerprint.getAllValues().get(0)).isEqualTo(fingerprint.getAllValues().get(1)).hasSize(64);
    }

    @Test
    void createProduct_missingOrInvalidKey_failsBeforeTouchingAnyService() {
        assertThatThrownBy(() -> controller.createProduct(null, createRequest())).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> controller.createProduct("not-a-uuid", createRequest()))
                .isInstanceOf(RuntimeException.class);

        verifyNoInteractions(idempotencyService, productService);
    }

    @Test
    void getProduct_noConditionalHeader_returns200WithETagAndCacheControl() {
        when(productService.findById(ID)).thenReturn(product);

        ResponseEntity<ProductResponse> response =
                controller.getProduct(ID, new ServletWebRequest(new MockHttpServletRequest("GET", "/x")));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getETag()).isEqualTo("\"3\"");
        assertThat(response.getHeaders().getCacheControl()).contains("public").contains("max-age=");
        assertThat(response.getBody()).isEqualTo(product);
    }

    @Test
    void getProduct_matchingIfNoneMatch_returns304WithoutBody() {
        when(productService.findById(ID)).thenReturn(product);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/x");
        request.addHeader(HttpHeaders.IF_NONE_MATCH, "\"3\"");

        ResponseEntity<ProductResponse> response = controller.getProduct(ID, new ServletWebRequest(request));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_MODIFIED);
        assertThat(response.getBody()).isNull();
        assertThat(response.getHeaders().getETag()).isEqualTo("\"3\"");
    }

    @Test
    void getProduct_staleIfNoneMatch_returns200() {
        when(productService.findById(ID)).thenReturn(product);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/x");
        request.addHeader(HttpHeaders.IF_NONE_MATCH, "\"2\"");

        ResponseEntity<ProductResponse> response = controller.getProduct(ID, new ServletWebRequest(request));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void updateProduct_ifMatch_passesParsedVersionAndReturnsNewETag() {
        ProductUpdateRequest update = new ProductUpdateRequest("ELECMS001", "Mouse", null, "Electronics",
                new BigDecimal("24.99"), 10, null);
        when(productService.update(eq("ELECMS001"), eq(update), any())).thenReturn(product);

        ResponseEntity<ProductResponse> response = controller.updateProduct("ELECMS001", "\"2\"", update);

        verify(productService).update("ELECMS001", update, java.util.Optional.of(2L));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getETag()).isEqualTo("\"3\"");
    }

    @Test
    void updateProduct_withoutIfMatch_passesEmptyVersion() {
        ProductUpdateRequest update = new ProductUpdateRequest(null, "Mouse", null, "Electronics",
                new BigDecimal("24.99"), 10, null);
        when(productService.update(any(), any(), any())).thenReturn(product);

        controller.updateProduct(ID.toString(), null, update);

        verify(productService).update(ID.toString(), update, java.util.Optional.empty());
    }

    @Test
    void deleteProduct_existing_returns204() {
        ResponseEntity<Void> response = controller.deleteProduct("ELECMS001");

        verify(productService).delete("ELECMS001");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void importCsv_withoutIdempotencyKey_importsDirectlyWithoutIdempotency() {
        when(importService.importCsv(any(), any())).thenReturn(report);
        MockMultipartFile file = new MockMultipartFile("file", "p.csv", "text/csv", "a,b".getBytes());

        ResponseEntity<CsvImportReport> response = controller.importCsv(null, file);

        assertThat(response.getBody()).isSameAs(report);
        assertThat(response.getHeaders().getFirst("Idempotent-Replayed")).isNull();
        verifyNoInteractions(idempotencyService);
        ArgumentCaptor<CsvUploadMetadata> metadata = ArgumentCaptor.forClass(CsvUploadMetadata.class);
        verify(importService).importCsv(metadata.capture(), any());
        assertThat(metadata.getValue()).usingRecursiveComparison()
                .isEqualTo(new CsvUploadMetadata("p.csv", "text/csv", 3L));
    }

    @Test
    @SuppressWarnings("unchecked")
    void importCsv_withKey_usesImportScopeAndConfiguredLockTtl() {
        when(idempotencyService.execute(anyString(), anyString(), anyString(), any(Class.class), any(),
                any(Supplier.class))).thenReturn(new IdempotencyExecutionResult<>(report, true));
        MockMultipartFile file = new MockMultipartFile("file", "p.csv", "text/csv", "a,b".getBytes());

        ResponseEntity<CsvImportReport> response = controller.importCsv(KEY, file);

        verify(idempotencyService).execute(eq("product-import"), eq(KEY), anyString(), eq(CsvImportReport.class),
                eq(ingestion.idempotencyLockTtl()), any(Supplier.class));
        assertThat(response.getHeaders().getFirst("Idempotent-Replayed")).isEqualTo("true");
        verify(importService, never()).importCsv(any(), any());
    }

    @Test
    void importCsv_fingerprintIsTheContentHash_notNameOrSize() throws Exception {
        String fingerprintA = importFingerprint("a.csv", "AAAA");
        String fingerprintSameNameSizeOtherContent = importFingerprint("a.csv", "BBBB");
        String fingerprintSameContentOtherName = importFingerprint("z.csv", "AAAA");

        assertThat(fingerprintSameNameSizeOtherContent).isNotEqualTo(fingerprintA);
        assertThat(fingerprintSameContentOtherName).isEqualTo(fingerprintA);
        assertThat(fingerprintA).isEqualTo(HashUtil.sha256Hex(new ByteArrayInputStream("AAAA".getBytes())));
    }

    @SuppressWarnings("unchecked")
    private String importFingerprint(String name, String content) {
        IdempotencyService service = mock(IdempotencyService.class);
        ProductController local = new ProductController(productService, mock(ProductCatalogService.class),
                importService, service, ingestion, JsonMapper.builder().build());
        when(service.execute(anyString(), anyString(), anyString(), any(Class.class), any(), any(Supplier.class)))
                .thenReturn(new IdempotencyExecutionResult<>(report, false));
        local.importCsv(KEY, new MockMultipartFile("file", name, "text/csv", content.getBytes()));
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(service).execute(anyString(), anyString(), captor.capture(), any(Class.class), any(),
                any(Supplier.class));
        return captor.getValue();
    }
}
