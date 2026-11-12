package com.gila.api.ecommerce.controller;

import com.gila.api.ecommerce.configuration.constants.GlobalConstants;
import com.gila.api.ecommerce.configuration.properties.IngestionProperties;
import com.gila.api.ecommerce.error.exceptions.InvalidCsvFileException;
import com.gila.api.ecommerce.model.cache.IdempotencyExecutionResult;
import com.gila.api.ecommerce.model.dto.CustomPageRequest;
import com.gila.api.ecommerce.model.dto.PageableResponse;
import com.gila.api.ecommerce.model.dto.product.*;
import com.gila.api.ecommerce.model.error.CustomErrorResponse;
import com.gila.api.ecommerce.service.cache.IdempotencyService;
import com.gila.api.ecommerce.service.product.ProductCatalogService;
import com.gila.api.ecommerce.service.product.ProductImportService;
import com.gila.api.ecommerce.service.product.ProductService;
import com.gila.api.ecommerce.util.CatalogPageables;
import com.gila.api.ecommerce.util.HashUtil;
import com.gila.api.ecommerce.util.HttpHeaderUtil;
import com.gila.api.ecommerce.util.QueryParameters;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.jspecify.annotations.Nullable;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping(GlobalConstants.API_V1_PRODUCTS)
@Tag(name = "Products", description = "Product lifecycle, catalog queries and bulk CSV import")
public class ProductController {

    static final String CREATE_SCOPE = "product-create";
    static final String IMPORT_SCOPE = "product-import";

    static final Set<String> FILTER_PARAMS =
            Set.of("name", "sku", "category", "minPrice", "maxPrice", "minStock", "minWeightKg", "maxWeightKg");

    private final ProductService productService;
    private final ProductCatalogService catalogService;
    private final ProductImportService importService;
    private final IdempotencyService idempotencyService;
    private final IngestionProperties ingestionProperties;
    private final JsonMapper jsonMapper;

    public ProductController(
            ProductService productService, ProductCatalogService catalogService,
            ProductImportService importService, IdempotencyService idempotencyService,
            IngestionProperties ingestionProperties, JsonMapper jsonMapper) {
        this.productService = productService;
        this.catalogService = catalogService;
        this.importService = importService;
        this.idempotencyService = idempotencyService;
        this.ingestionProperties = ingestionProperties;
        this.jsonMapper = jsonMapper;
    }

    @GetMapping(value = "/filter", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Search and filter products",
            description = "AND-combined parameters, all optional: name (case-insensitive substring match on the "
                    + "product name; whitespace-separated words are combined with OR, e.g. 'camp tent'; no stemming "
                    + "or typo tolerance; max 100 characters), sku (contains, 3-10 chars A-Z0-9), category (exact, "
                    + "case-insensitive), minPrice/maxPrice, minStock (stock >= value) and minWeightKg/maxWeightKg "
                    + "(inclusive). Only active products. sortBy: createdAt, price, name (default createdAt DESC). "
                    + "Unknown query parameters are rejected with 400. Pages are cached server side for 30 seconds "
                    + "(eventual consistency).")
    @ApiResponse(responseCode = "200", description = "Product page, possibly empty")
    @ApiResponse(responseCode = "400", description = "Invalid name, filters, sort, pagination or unknown parameter",
            content = @Content(schema = @Schema(implementation = CustomErrorResponse.class)))
    public ResponseEntity<PageableResponse<ProductSearchDTO>> filterProducts(
            @ParameterObject @Valid @ModelAttribute ProductFilterParams params,
            @ParameterObject @Valid @ModelAttribute CustomPageRequest pageRequest,
            @Parameter(hidden = true) HttpServletRequest request) {
        QueryParameters.requireOnly(request, FILTER_PARAMS);
        return ResponseEntity.ok()
                .cacheControl(CacheControl
                        .maxAge(GlobalConstants.PRODUCT_FILTER_CACHE_MAX_AGE_SECONDS, TimeUnit.SECONDS)
                        .cachePublic())
                .body(catalogService.filter(params, CatalogPageables.forFilter(pageRequest)));
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get product detail by UUID",
            description = "Only the UUID identifies a product here (SKU is not accepted). Supports conditional "
                    + "requests through If-None-Match using the version based ETag.")
    @ApiResponse(responseCode = "200", description = "Product found")
    @ApiResponse(responseCode = "400", description = "The identifier is not a valid UUID",
            content = @Content(schema = @Schema(implementation = CustomErrorResponse.class)))
    @ApiResponse(responseCode = "304", description = "Product not modified since the provided ETag")
    @ApiResponse(responseCode = "404", description = "Product not found or soft-deleted",
            content = @Content(schema = @Schema(implementation = CustomErrorResponse.class)))
    public ResponseEntity<ProductResponse> getProduct(
            @Parameter(description = "Product UUID", example = "3f1c1a52-5d0e-4bb8-9a7a-0f6f4f3b9c11")
            @PathVariable UUID id,
            WebRequest webRequest) {
        ProductResponse product = productService.findById(id);
        String eTag = HttpHeaderUtil.toETag(product.version());
        CacheControl cacheControl = CacheControl
                .maxAge(GlobalConstants.PRODUCT_DETAIL_CACHE_MAX_AGE_SECONDS, TimeUnit.SECONDS)
                .cachePublic();
        if (webRequest.checkNotModified(eTag)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).cacheControl(cacheControl).eTag(eTag).build();
        }
        return ResponseEntity.ok().cacheControl(cacheControl).eTag(eTag).body(product);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create a product",
            description = "Requires an Idempotency-Key (UUID). Retries with the same key and payload replay the "
                    + "original 201 response with the Idempotent-Replayed header.")
    @Parameter(name = GlobalConstants.HEADER_IDEMPOTENCY_KEY, in = ParameterIn.HEADER, required = true,
            description = "Client generated UUID identifying this creation attempt",
            example = "3f1c1a52-5d0e-4bb8-9a7a-0f6f4f3b9c11")
    @ApiResponse(responseCode = "201", description = "Product created")
    @ApiResponse(responseCode = "400", description = "Invalid payload, category or Idempotency-Key",
            content = @Content(schema = @Schema(implementation = CustomErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Duplicate SKU or request with the same key still in flight",
            content = @Content(schema = @Schema(implementation = CustomErrorResponse.class)))
    @ApiResponse(responseCode = "422", description = "Idempotency-Key reused with a different payload",
            content = @Content(schema = @Schema(implementation = CustomErrorResponse.class)))
    public ResponseEntity<ProductResponse> createProduct(
            @Parameter(hidden = true)
            @RequestHeader(value = GlobalConstants.HEADER_IDEMPOTENCY_KEY, required = false) @Nullable String rawKey,
            @Valid @RequestBody ProductCreateRequest request) {
        String idempotencyKey = HttpHeaderUtil.requireIdempotencyKey(rawKey);
        String fingerprint = HashUtil.sha256Hex(jsonMapper.writeValueAsString(request));
        IdempotencyExecutionResult<ProductResponse> result = idempotencyService.execute(
                CREATE_SCOPE, idempotencyKey, fingerprint, ProductResponse.class,
                () -> productService.create(request));
        ProductResponse product = result.payload();
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(product.id())
                .toUri();
        ResponseEntity.BodyBuilder builder = ResponseEntity.created(location)
                .eTag(HttpHeaderUtil.toETag(product.version()));
        if (result.isReplay()) {
            builder.header(GlobalConstants.HEADER_IDEMPOTENT_REPLAYED, Boolean.TRUE.toString());
        }
        return builder.body(product);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Fully replace a product",
            description = "Idempotent by definition. SKU is immutable. Send If-Match with the current ETag to "
                    + "enable optimistic concurrency control.")
    @ApiResponse(responseCode = "200", description = "Product updated")
    @ApiResponse(responseCode = "400", description = "Invalid payload, category, If-Match or SKU change attempt",
            content = @Content(schema = @Schema(implementation = CustomErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Product not found or soft-deleted",
            content = @Content(schema = @Schema(implementation = CustomErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "If-Match does not match the current version",
            content = @Content(schema = @Schema(implementation = CustomErrorResponse.class)))
    public ResponseEntity<ProductResponse> updateProduct(
            @Parameter(description = "Product UUID or SKU", example = "ELECMS001") @PathVariable String id,
            @Parameter(description = "Current product ETag, e.g. \"3\"")
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) @Nullable String ifMatch,
            @Valid @RequestBody ProductUpdateRequest request) {
        ProductResponse product = productService.update(id, request, HttpHeaderUtil.parseIfMatch(ifMatch));
        return ResponseEntity.ok()
                .eTag(HttpHeaderUtil.toETag(product.version()))
                .body(product);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a product", description = "Marks the product as inactive and keeps history.")
    @ApiResponse(responseCode = "204", description = "Product soft-deleted")
    @ApiResponse(responseCode = "404", description = "Product not found or already deleted",
            content = @Content(schema = @Schema(implementation = CustomErrorResponse.class)))
    public ResponseEntity<Void> deleteProduct(
            @Parameter(description = "Product UUID or SKU", example = "ELECMS001") @PathVariable String id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/import-csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Bulk import products from CSV",
            description = "Streams the file and upserts by SKU in batches. Header: name,description,sku,category,"
                    + "price,stock,weight_kg (description and weight_kg optional). Invalid rows are skipped and "
                    + "reported. An optional Idempotency-Key (UUID) replays the first report.")
    @ApiResponse(responseCode = "200", description = "Import report (possibly with rejected rows)")
    @ApiResponse(responseCode = "400", description = "Missing, empty or non CSV file, or invalid header",
            content = @Content(schema = @Schema(implementation = CustomErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "An import with the same Idempotency-Key is still running",
            content = @Content(schema = @Schema(implementation = CustomErrorResponse.class)))
    @ApiResponse(responseCode = "413", description = "File exceeds the maximum upload size",
            content = @Content(schema = @Schema(implementation = CustomErrorResponse.class)))
    @ApiResponse(responseCode = "422", description = "No valid row found, or Idempotency-Key reused with another file",
            content = @Content(schema = @Schema(implementation = CustomErrorResponse.class)))
    public ResponseEntity<CsvImportReport> importCsv(
            @Parameter(description = "Optional client generated UUID")
            @RequestHeader(value = GlobalConstants.HEADER_IDEMPOTENCY_KEY, required = false) @Nullable String rawKey,
            @Parameter(description = "CSV file") @RequestPart("file") MultipartFile file) {
        CsvUploadMetadata metadata = new CsvUploadMetadata(file.getOriginalFilename(), file.getContentType(),
                file.getSize());
        Optional<String> idempotencyKey = HttpHeaderUtil.optionalIdempotencyKey(rawKey);
        if (idempotencyKey.isEmpty()) {
            return ResponseEntity.ok(runImport(metadata, file));
        }
        String fingerprint = contentFingerprint(file);
        IdempotencyExecutionResult<CsvImportReport> result = idempotencyService.execute(
                IMPORT_SCOPE, idempotencyKey.get(), fingerprint, CsvImportReport.class,
                ingestionProperties.idempotencyLockTtl(), () -> runImport(metadata, file));
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok();
        if (result.isReplay()) {
            builder.header(GlobalConstants.HEADER_IDEMPOTENT_REPLAYED, Boolean.TRUE.toString());
        }
        return builder.body(result.payload());
    }

    private static String contentFingerprint(MultipartFile file) {
        try (InputStream input = file.getInputStream()) {
            return HashUtil.sha256Hex(input);
        } catch (IOException exception) {
            throw new InvalidCsvFileException("The uploaded file could not be opened.", exception);
        }
    }

    private CsvImportReport runImport(CsvUploadMetadata metadata, MultipartFile file) {
        try (InputStream input = file.getInputStream()) {
            return importService.importCsv(metadata, input);
        } catch (IOException exception) {
            throw new InvalidCsvFileException("The uploaded file could not be opened.", exception);
        }
    }
}
