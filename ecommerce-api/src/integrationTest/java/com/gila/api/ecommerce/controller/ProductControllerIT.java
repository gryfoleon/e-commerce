package com.gila.api.ecommerce.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gila.api.ecommerce.configuration.IntegrationTestContainersConfiguration;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("IT")
@Import(IntegrationTestContainersConfiguration.class)
class ProductControllerIT {

    private static final String BASE = "/v1/products";
    private static final String IDEMPOTENCY_KEY = "Idempotency-Key";
    private static final String REPLAYED = "Idempotent-Replayed";

    private static final String LAPTOP_ID = "11111111-1111-1111-1111-111111111111";
    private static final String INACTIVE_ID = "44444444-4444-4444-4444-444444444444";

    private static final String TEST_CATEGORY = "Pets";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Test
    void filter_returnsOnlyActiveProductsOfCategoryIgnoringCase() throws Exception {
        mockMvc.perform(get(BASE + "/filter").param("category", "electronics"))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.CACHE_CONTROL))
                .andExpect(jsonPath("$.content[*].sku", hasItems("LAPTOP01", "MOUSE001")))
                .andExpect(jsonPath("$.content[*].sku", not(hasItem("OLDITEM1"))))
                .andExpect(jsonPath("$.content[*].category", not(hasItem("Books"))));
    }

    @Test
    void filter_searchesByNameSubstringIgnoringCase() throws Exception {
        mockMvc.perform(get(BASE + "/filter").param("name", "WIRELESS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].sku").value("MOUSE001"));
    }

    @Test
    void filter_combinesNameWordsWithOr() throws Exception {
        mockMvc.perform(get(BASE + "/filter").param("name", "laptop clean"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].sku", hasItems("LAPTOP01", "BOOK0001")));
    }

    @Test
    void filter_searchesBySkuFragment() throws Exception {
        mockMvc.perform(get(BASE + "/filter").param("sku", "LAPTOP"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(LAPTOP_ID));
    }

    @Test
    void filter_appliesPriceRange() throws Exception {
        mockMvc.perform(get(BASE + "/filter").param("category", "Electronics")
                        .param("minPrice", "20").param("maxPrice", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].sku").value("MOUSE001"));
    }

    @Test
    void filter_appliesMinStock() throws Exception {
        mockMvc.perform(get(BASE + "/filter").param("category", "Electronics").param("minStock", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].sku").value("MOUSE001"));
    }

    @Test
    void filter_appliesWeightRange() throws Exception {
        mockMvc.perform(get(BASE + "/filter").param("category", "Electronics")
                        .param("minWeightKg", "1").param("maxWeightKg", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].sku").value("LAPTOP01"));
    }

    @Test
    void filter_sortsByPriceDescending() throws Exception {
        mockMvc.perform(get(BASE + "/filter").param("category", "Electronics")
                        .param("sortBy", "price").param("sortDirection", "DESC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].sku").value("LAPTOP01"))
                .andExpect(jsonPath("$.content[1].sku").value("MOUSE001"));
    }

    @Test
    void filter_paginates() throws Exception {
        mockMvc.perform(get(BASE + "/filter").param("category", "Electronics")
                        .param("sortBy", "name").param("size", "1").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].sku").value("MOUSE001"))
                .andExpect(jsonPath("$.pageDetails.page").value(1))
                .andExpect(jsonPath("$.pageDetails.size").value(1))
                .andExpect(jsonPath("$.pageDetails.totalElements").value(2))
                .andExpect(jsonPath("$.pageDetails.totalPages").value(2))
                .andExpect(jsonPath("$.pageDetails.isLast").value(true));
    }

    @Test
    void filter_returnsEmptyPageWhenNothingMatches() throws Exception {
        mockMvc.perform(get(BASE + "/filter").param("name", "zzzznomatch"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.pageDetails.totalElements").value(0));
    }

    @Test
    void filter_rejectsUnknownParameter() throws Exception {
        mockMvc.perform(get(BASE + "/filter").param("foo", "bar"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void filter_rejectsUnknownSortField() throws Exception {
        mockMvc.perform(get(BASE + "/filter").param("sortBy", "stock"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void filter_rejectsInvertedPriceRange() throws Exception {
        mockMvc.perform(get(BASE + "/filter").param("minPrice", "100").param("maxPrice", "10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void filter_rejectsInvalidSkuFilter() throws Exception {
        mockMvc.perform(get(BASE + "/filter").param("sku", "ab"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getProduct_returnsSeededProductWithEtagAndCacheControl() throws Exception {
        mockMvc.perform(get(BASE + "/{id}", LAPTOP_ID))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.ETAG))
                .andExpect(header().exists(HttpHeaders.CACHE_CONTROL))
                .andExpect(jsonPath("$.id").value(LAPTOP_ID))
                .andExpect(jsonPath("$.sku").value("LAPTOP01"))
                .andExpect(jsonPath("$.name").value("Laptop Pro 15"))
                .andExpect(jsonPath("$.description").value("High-end laptop"))
                .andExpect(jsonPath("$.category").value("Electronics"))
                .andExpect(jsonPath("$.price").value(1499.99))
                .andExpect(jsonPath("$.stock").value(10))
                .andExpect(jsonPath("$.weightKg").value(1.8));
    }

    @Test
    void getProduct_returnsNotModifiedWhenEtagMatches() throws Exception {
        String eTag = mockMvc.perform(get(BASE + "/{id}", LAPTOP_ID))
                .andExpect(status().isOk())
                .andReturn().getResponse().getHeader(HttpHeaders.ETAG);

        mockMvc.perform(get(BASE + "/{id}", LAPTOP_ID).header(HttpHeaders.IF_NONE_MATCH, eTag))
                .andExpect(status().isNotModified())
                .andExpect(header().string(HttpHeaders.ETAG, eTag));
    }

    @Test
    void getProduct_returnsNotFoundForSoftDeletedProduct() throws Exception {
        mockMvc.perform(get(BASE + "/{id}", INACTIVE_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value(404));
    }

    @Test
    void getProduct_returnsNotFoundForUnknownId() throws Exception {
        mockMvc.perform(get(BASE + "/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getProduct_returnsBadRequestForInvalidUuid() throws Exception {
        mockMvc.perform(get(BASE + "/{id}", "LAPTOP01"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_returnsCreatedWithLocationAndEtag() throws Exception {
        String sku = uniqueSku();

        MvcResult result = mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .header(IDEMPOTENCY_KEY, UUID.randomUUID().toString())
                        .content(productBody(sku, "Created Product", TEST_CATEGORY, "19.90", 5, "0.250")))
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.ETAG))
                .andExpect(header().doesNotExist(REPLAYED))
                .andExpect(jsonPath("$.sku").value(sku))
                .andExpect(jsonPath("$.category").value(TEST_CATEGORY))
                .andExpect(jsonPath("$.price").value(19.9))
                .andExpect(jsonPath("$.stock").value(5))
                .andExpect(jsonPath("$.weightKg").value(0.25))
                .andReturn();

        String id = readJson(result).get("id").asString();
        org.assertj.core.api.Assertions.assertThat(result.getResponse().getHeader(HttpHeaders.LOCATION))
                .endsWith(BASE + "/" + id);
        mockMvc.perform(get(BASE + "/{id}", id)).andExpect(status().isOk());
    }

    @Test
    void create_normalizesCategoryToCanonicalName() throws Exception {
        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .header(IDEMPOTENCY_KEY, UUID.randomUUID().toString())
                        .content(productBody(uniqueSku(), "Lowercase Category", "pets", "10.00", 1, null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.category").value(TEST_CATEGORY))
                .andExpect(jsonPath("$.weightKg").doesNotExist());
    }

    @Test
    void create_replaysResponseForSameKeyAndPayload() throws Exception {
        String key = UUID.randomUUID().toString();
        String body = productBody(uniqueSku(), "Replayed Product", TEST_CATEGORY, "12.00", 2, null);

        String firstId = readJson(mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .header(IDEMPOTENCY_KEY, key).content(body))
                .andExpect(status().isCreated())
                .andReturn()).get("id").asString();

        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .header(IDEMPOTENCY_KEY, key).content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string(REPLAYED, "true"))
                .andExpect(jsonPath("$.id").value(firstId));
    }

    @Test
    void create_returnsUnprocessableWhenKeyIsReusedWithDifferentPayload() throws Exception {
        String key = UUID.randomUUID().toString();
        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).header(IDEMPOTENCY_KEY, key)
                        .content(productBody(uniqueSku(), "First Payload", TEST_CATEGORY, "12.00", 2, null)))
                .andExpect(status().isCreated());

        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).header(IDEMPOTENCY_KEY, key)
                        .content(productBody(uniqueSku(), "Second Payload", TEST_CATEGORY, "12.00", 2, null)))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void create_returnsBadRequestWithoutIdempotencyKey() throws Exception {
        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content(productBody(uniqueSku(), "No Key", TEST_CATEGORY, "10.00", 1, null)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_returnsBadRequestForInvalidIdempotencyKey() throws Exception {
        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).header(IDEMPOTENCY_KEY, "not-a-uuid")
                        .content(productBody(uniqueSku(), "Bad Key", TEST_CATEGORY, "10.00", 1, null)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_returnsConflictForExistingSku() throws Exception {
        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .header(IDEMPOTENCY_KEY, UUID.randomUUID().toString())
                        .content(productBody("LAPTOP01", "Duplicate", TEST_CATEGORY, "10.00", 1, null)))
                .andExpect(status().isConflict());
    }

    @Test
    void create_returnsConflictForSkuOfSoftDeletedProduct() throws Exception {
        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .header(IDEMPOTENCY_KEY, UUID.randomUUID().toString())
                        .content(productBody("OLDITEM1", "Duplicate", TEST_CATEGORY, "10.00", 1, null)))
                .andExpect(status().isConflict());
    }

    @Test
    void create_returnsBadRequestForUnknownCategory() throws Exception {
        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .header(IDEMPOTENCY_KEY, UUID.randomUUID().toString())
                        .content(productBody(uniqueSku(), "Bad Category", "Nonexistent", "10.00", 1, null)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_returnsBadRequestForInvalidPayload() throws Exception {
        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .header(IDEMPOTENCY_KEY, UUID.randomUUID().toString())
                        .content(productBody("bad sku!", "", TEST_CATEGORY, "0", -1, null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400));
    }

    @Test
    void update_replacesProductById() throws Exception {
        JsonNode created = createProduct("Before Update", "10.00", 1);
        String id = created.get("id").asString();

        mockMvc.perform(put(BASE + "/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content(productBody(null, "After Update", "Books", "55.50", 9, "2.000")))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.ETAG))
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.sku").value(created.get("sku").asString()))
                .andExpect(jsonPath("$.name").value("After Update"))
                .andExpect(jsonPath("$.category").value("Books"))
                .andExpect(jsonPath("$.price").value(55.5))
                .andExpect(jsonPath("$.stock").value(9));

        mockMvc.perform(get(BASE + "/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("After Update"));
    }

    @Test
    void update_acceptsSkuAsIdentifierAndSameSkuInBody() throws Exception {
        String sku = createProduct("By Sku", "10.00", 1).get("sku").asString();

        mockMvc.perform(put(BASE + "/{id}", sku).contentType(MediaType.APPLICATION_JSON)
                        .content(productBody(sku, "Renamed By Sku", TEST_CATEGORY, "11.00", 2, null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed By Sku"));
    }

    @Test
    void update_returnsBadRequestWhenChangingSku() throws Exception {
        String id = createProduct("Immutable Sku", "10.00", 1).get("id").asString();

        mockMvc.perform(put(BASE + "/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content(productBody(uniqueSku(), "Immutable Sku", TEST_CATEGORY, "10.00", 1, null)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_succeedsWhenIfMatchEqualsCurrentVersion() throws Exception {
        JsonNode created = createProduct("If Match Ok", "10.00", 1);
        MvcResult current = mockMvc.perform(get(BASE + "/{id}", created.get("id").asString()))
                .andExpect(status().isOk()).andReturn();
        String eTag = current.getResponse().getHeader(HttpHeaders.ETAG);

        mockMvc.perform(put(BASE + "/{id}", created.get("id").asString()).contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.IF_MATCH, eTag)
                        .content(productBody(null, "If Match Updated", TEST_CATEGORY, "10.00", 1, null)))
                .andExpect(status().isOk());
    }

    @Test
    void update_returnsConflictWhenIfMatchIsStale() throws Exception {
        String id = createProduct("If Match Stale", "10.00", 1).get("id").asString();

        mockMvc.perform(put(BASE + "/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.IF_MATCH, "\"999\"")
                        .content(productBody(null, "If Match Stale", TEST_CATEGORY, "10.00", 1, null)))
                .andExpect(status().isConflict());
    }

    @Test
    void update_returnsBadRequestForMalformedIfMatch() throws Exception {
        String id = createProduct("If Match Bad", "10.00", 1).get("id").asString();

        mockMvc.perform(put(BASE + "/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.IF_MATCH, "\"abc\"")
                        .content(productBody(null, "If Match Bad", TEST_CATEGORY, "10.00", 1, null)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_returnsNotFoundForUnknownProduct() throws Exception {
        mockMvc.perform(put(BASE + "/{id}", UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON)
                        .content(productBody(null, "Ghost", TEST_CATEGORY, "10.00", 1, null)))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_returnsNotFoundForSoftDeletedProduct() throws Exception {
        mockMvc.perform(put(BASE + "/{id}", INACTIVE_ID).contentType(MediaType.APPLICATION_JSON)
                        .content(productBody(null, "Ghost", TEST_CATEGORY, "10.00", 1, null)))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_returnsBadRequestForUnknownCategory() throws Exception {
        String id = createProduct("Bad Category Update", "10.00", 1).get("id").asString();

        mockMvc.perform(put(BASE + "/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content(productBody(null, "Bad Category Update", "Nonexistent", "10.00", 1, null)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_returnsBadRequestForInvalidPayload() throws Exception {
        String id = createProduct("Invalid Update", "10.00", 1).get("id").asString();

        mockMvc.perform(put(BASE + "/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content(productBody(null, " ", TEST_CATEGORY, "-5", -1, null)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void delete_softDeletesProductById() throws Exception {
        String id = createProduct("Delete By Id", "10.00", 1).get("id").asString();

        mockMvc.perform(delete(BASE + "/{id}", id)).andExpect(status().isNoContent());

        mockMvc.perform(get(BASE + "/{id}", id)).andExpect(status().isNotFound());
        mockMvc.perform(delete(BASE + "/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    void delete_softDeletesProductBySku() throws Exception {
        JsonNode created = createProduct("Delete By Sku", "10.00", 1);

        mockMvc.perform(delete(BASE + "/{id}", created.get("sku").asString())).andExpect(status().isNoContent());

        mockMvc.perform(get(BASE + "/{id}", created.get("id").asString())).andExpect(status().isNotFound());
    }

    @Test
    void delete_removesProductFromFilterResults() throws Exception {
        JsonNode created = createProduct("Filter Then Delete", "10.00", 1);
        String sku = created.get("sku").asString();

        mockMvc.perform(get(BASE + "/filter").param("sku", sku))
                .andExpect(jsonPath("$.content.length()").value(1));

        mockMvc.perform(delete(BASE + "/{id}", sku)).andExpect(status().isNoContent());

        mockMvc.perform(get(BASE + "/filter").param("sku", sku))
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void delete_returnsNotFoundForUnknownProduct() throws Exception {
        mockMvc.perform(delete(BASE + "/{id}", UUID.randomUUID())).andExpect(status().isNotFound());
    }

    @Test
    void delete_returnsNotFoundForAlreadySoftDeletedProduct() throws Exception {
        mockMvc.perform(delete(BASE + "/{id}", INACTIVE_ID)).andExpect(status().isNotFound());
    }

    @Test
    void importCsv_insertsRowsAndReportsRejectedOnes() throws Exception {
        String skuA = uniqueSku();
        String skuB = uniqueSku();
        String csv = """
                name,description,sku,category,price,stock,weight_kg
                Imported A,First,%s,Pets,10.50,3,0.5
                Imported B,,%s,pets,20.00,4,
                Invalid Row,,%s,Pets,-1,1,
                """.formatted(skuA, skuB, uniqueSku());

        mockMvc.perform(multipart(BASE + "/import-csv").file(csvFile(csv)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRows").value(3))
                .andExpect(jsonPath("$.insertedRows").value(2))
                .andExpect(jsonPath("$.updatedRows").value(0))
                .andExpect(jsonPath("$.rejectedRows").value(1))
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].field").value("price"));

        mockMvc.perform(get(BASE + "/filter").param("sku", skuB))
                .andExpect(jsonPath("$.content[0].category").value(TEST_CATEGORY))
                .andExpect(jsonPath("$.content[0].price").value(20.0));
    }

    @Test
    void importCsv_upsertsExistingSku() throws Exception {
        String sku = uniqueSku();
        String header = "name,sku,category,price,stock\n";
        mockMvc.perform(multipart(BASE + "/import-csv")
                        .file(csvFile(header + "Upsert Original," + sku + ",Pets,10.00,1\n")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.insertedRows").value(1));

        mockMvc.perform(multipart(BASE + "/import-csv")
                        .file(csvFile(header + "Upsert Changed," + sku + ",Pets,15.00,7\n")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.insertedRows").value(0))
                .andExpect(jsonPath("$.updatedRows").value(1));

        mockMvc.perform(get(BASE + "/filter").param("sku", sku))
                .andExpect(jsonPath("$.content[0].name").value("Upsert Changed"))
                .andExpect(jsonPath("$.content[0].stock").value(7))
                .andExpect(jsonPath("$.content[0].price").value(15.0));
    }

    @Test
    void importCsv_replaysReportForSameIdempotencyKeyAndFile() throws Exception {
        String key = UUID.randomUUID().toString();
        String csv = "name,sku,category,price,stock\nIdempotent Import," + uniqueSku() + ",Pets,10.00,1\n";

        mockMvc.perform(multipart(BASE + "/import-csv").file(csvFile(csv)).header(IDEMPOTENCY_KEY, key))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist(REPLAYED))
                .andExpect(jsonPath("$.insertedRows").value(1));

        mockMvc.perform(multipart(BASE + "/import-csv").file(csvFile(csv)).header(IDEMPOTENCY_KEY, key))
                .andExpect(status().isOk())
                .andExpect(header().string(REPLAYED, "true"))
                .andExpect(jsonPath("$.insertedRows").value(1));
    }

    @Test
    void importCsv_returnsUnprocessableWhenKeyIsReusedWithAnotherFile() throws Exception {
        String key = UUID.randomUUID().toString();
        String header = "name,sku,category,price,stock\n";
        mockMvc.perform(multipart(BASE + "/import-csv")
                        .file(csvFile(header + "Key Reuse A," + uniqueSku() + ",Pets,10.00,1\n"))
                        .header(IDEMPOTENCY_KEY, key))
                .andExpect(status().isOk());

        mockMvc.perform(multipart(BASE + "/import-csv")
                        .file(csvFile(header + "Key Reuse B," + uniqueSku() + ",Pets,10.00,1\n"))
                        .header(IDEMPOTENCY_KEY, key))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void importCsv_returnsUnprocessableWhenNoRowIsValid() throws Exception {
        String csv = "name,sku,category,price,stock\nBad,bad sku,Pets,-1,1\nWorse,,Nope,abc,x\n";

        mockMvc.perform(multipart(BASE + "/import-csv").file(csvFile(csv)))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void importCsv_returnsBadRequestForMissingRequiredColumn() throws Exception {
        mockMvc.perform(multipart(BASE + "/import-csv")
                        .file(csvFile("name,sku,category,price\nNo Stock," + uniqueSku() + ",Pets,10.00\n")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void importCsv_returnsBadRequestForFileWithoutDataRows() throws Exception {
        mockMvc.perform(multipart(BASE + "/import-csv").file(csvFile("name,sku,category,price,stock\n")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void importCsv_returnsBadRequestForEmptyFile() throws Exception {
        mockMvc.perform(multipart(BASE + "/import-csv").file(csvFile("")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void importCsv_returnsBadRequestForNonCsvFile() throws Exception {
        MockMultipartFile pdf = new MockMultipartFile("file", "products.pdf", "application/pdf",
                "name,sku,category,price,stock\n".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart(BASE + "/import-csv").file(pdf))
                .andExpect(status().isBadRequest());
    }

    @Test
    void importCsv_returnsBadRequestWhenFilePartIsMissing() throws Exception {
        mockMvc.perform(multipart(BASE + "/import-csv"))
                .andExpect(status().isBadRequest());
    }

    private JsonNode createProduct(String name, String price, int stock) throws Exception {
        MvcResult result = mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .header(IDEMPOTENCY_KEY, UUID.randomUUID().toString())
                        .content(productBody(uniqueSku(), name, TEST_CATEGORY, price, stock, null)))
                .andExpect(status().isCreated())
                .andReturn();
        return readJson(result);
    }

    private JsonNode readJson(MvcResult result) throws Exception {
        return jsonMapper.readTree(result.getResponse().getContentAsString());
    }

    private String productBody(
            String sku, String name, String category, String price, int stock, String weightKg) {
        var node = jsonMapper.createObjectNode();
        if (sku != null) {
            node.put("sku", sku);
        }
        node.put("name", name);
        node.put("description", "Integration test product");
        node.put("category", category);
        node.put("price", new BigDecimal(price));
        node.put("stock", stock);
        if (weightKg != null) {
            node.put("weightKg", new BigDecimal(weightKg));
        }
        return node.toString();
    }

    private static MockMultipartFile csvFile(String content) {
        return new MockMultipartFile("file", "products.csv", "text/csv", content.getBytes(StandardCharsets.UTF_8));
    }

    private static String uniqueSku() {
        return "T" + UUID.randomUUID().toString().replace("-", "").substring(0, 9).toUpperCase(Locale.ROOT);
    }
}
