package com.gila.api.ecommerce.service.product.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.gila.api.ecommerce.configuration.properties.IngestionProperties;
import com.gila.api.ecommerce.error.exceptions.CsvIngestionRejectedException;
import com.gila.api.ecommerce.error.exceptions.InvalidCsvFileException;
import com.gila.api.ecommerce.model.dto.product.CsvImportReport;
import com.gila.api.ecommerce.model.dto.product.CsvRowError;
import com.gila.api.ecommerce.model.dto.product.CsvUploadMetadata;
import com.gila.api.ecommerce.model.query.ProductUpsertRow;
import com.gila.api.ecommerce.model.query.UpsertOutcome;
import com.gila.api.ecommerce.repository.product.ProductBatchRepository;
import com.gila.api.ecommerce.service.category.CategoryService;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductImportServiceImplTest {

    private static final String HEADER = "name,description,sku,category,price,stock,weight_kg\n";
    private static final CsvUploadMetadata CSV_METADATA = new CsvUploadMetadata("products.csv", "text/csv", 100);

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    @Mock
    private ProductBatchRepository batchRepository;
    @Mock
    private CategoryService categoryService;

    private List<List<ProductUpsertRow>> batches;

    @BeforeEach
    void setUp() {
        batches = Collections.synchronizedList(new ArrayList<>());
    }

    private ProductImportServiceImpl service(int batchSize, int maxErrors) {
        IngestionProperties properties = new IngestionProperties(batchSize, maxErrors, 2, Duration.ofMinutes(1));
        return new ProductImportServiceImpl(batchRepository, categoryService, VALIDATOR, properties);
    }

    private void knownCategories() {
        given(categoryService.findIdsByName()).willReturn(Map.of("Electronics", (short) 1, "Books", (short) 2));
    }

    private void persistAllAsInserted() {
        given(batchRepository.upsertBatch(anyList())).willAnswer(invocation -> {
            List<ProductUpsertRow> rows = invocation.getArgument(0);
            batches.add(rows);
            return new UpsertOutcome(rows.size(), 0);
        });
    }

    private CsvImportReport run(ProductImportServiceImpl service, String csv) {
        return service.importCsv(CSV_METADATA, new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));
    }

    private static String row(String sku, String category, String price, String stock) {
        return "Product " + sku + ",desc," + sku + "," + category + "," + price + "," + stock + ",0.5\n";
    }

    @Test
    void importCsv_emptyFile_throwsInvalidCsvFile() {
        CsvUploadMetadata empty = new CsvUploadMetadata("products.csv", "text/csv", 0);

        assertThatThrownBy(() -> service(10, 10).importCsv(empty, new ByteArrayInputStream(new byte[0])))
                .isInstanceOf(InvalidCsvFileException.class).hasMessageContaining("empty");
    }

    @ParameterizedTest
    @CsvSource(value = {"data.txt,application/pdf", "data.xlsx,image/png", "NULL,NULL"}, nullValues = "NULL")
    void importCsv_nonCsvFile_throwsInvalidCsvFile(String filename, String contentType) {
        CsvUploadMetadata metadata = new CsvUploadMetadata(filename, contentType, 10);

        assertThatThrownBy(() -> service(10, 10).importCsv(metadata, new ByteArrayInputStream(new byte[1])))
                .isInstanceOf(InvalidCsvFileException.class).hasMessageContaining("Only CSV");
    }

    @ParameterizedTest
    @CsvSource({"PRODUCTS.CSV,application/pdf", "file.bin,text/csv", "file.bin,'text/csv; charset=utf-8'",
            "file.bin,application/vnd.ms-excel"})
    void importCsv_csvByExtensionOrContentType_isAccepted(String filename, String contentType) {
        knownCategories();
        persistAllAsInserted();
        CsvUploadMetadata metadata = new CsvUploadMetadata(filename, contentType, 10);
        String csv = HEADER + row("ELEC1", "Electronics", "10.00", "5");

        CsvImportReport report = service(10, 10)
                .importCsv(metadata, new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));

        assertThat(report.insertedRows()).isEqualTo(1);
    }

    @Test
    void importCsv_headerMissingRequiredColumns_throwsListingThem() {
        String csv = "name,sku\nMouse,ELEC1\n";

        assertThatThrownBy(() -> run(service(10, 10), csv))
                .isInstanceOf(InvalidCsvFileException.class)
                .hasMessageContaining("category, price, stock");
        verify(batchRepository, never()).upsertBatch(anyList());
    }

    @Test
    void importCsv_reorderedAndUppercaseHeaders_areAccepted() {
        knownCategories();
        persistAllAsInserted();
        String csv = "STOCK,Price,CATEGORY,SKU,Name\n5,10.50,Electronics,ELEC1,Mouse\n";

        CsvImportReport report = run(service(10, 10), csv);

        assertThat(report.insertedRows()).isEqualTo(1);
        ProductUpsertRow persisted = batches.getFirst().getFirst();
        assertThat(persisted.sku()).isEqualTo("ELEC1");
        assertThat(persisted.name()).isEqualTo("Mouse");
        assertThat(persisted.stock()).isEqualTo(5);
        assertThat(persisted.description()).isNull();
        assertThat(persisted.weightKg()).isNull();
    }

    @Test
    void importCsv_utf8Bom_isIgnored() {
        knownCategories();
        persistAllAsInserted();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bytes.writeBytes(new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
        bytes.writeBytes((HEADER + row("ELEC1", "Electronics", "10.00", "5")).getBytes(StandardCharsets.UTF_8));

        CsvImportReport report = service(10, 10)
                .importCsv(CSV_METADATA, new ByteArrayInputStream(bytes.toByteArray()));

        assertThat(report.insertedRows()).isEqualTo(1);
        assertThat(report.rejectedRows()).isZero();
    }

    @Test
    void importCsv_categoryIsCaseInsensitive_andMappedToCategoryId() {
        knownCategories();
        persistAllAsInserted();

        run(service(10, 10), HEADER + row("ELEC1", "eLeCtRoNiCs", "10.00", "5") + row("BOOK1", "books", "9.99", "0"));

        assertThat(batches.getFirst()).extracting(ProductUpsertRow::categoryId).containsExactly((short) 2, (short) 1);
    }

    @ParameterizedTest
    @CsvSource({
            "abc,5,price,Value must be a decimal number.",
            "10.00,five,stock,Value must be an integer number.",
            "0,5,price,Price must be greater than zero",
            "10.00,-1,stock,Stock must be zero or positive",
            "10.123,5,price,Price supports up to 10 integer digits and 2 decimals"})
    void importCsv_invalidNumericValues_areRejectedWithFieldAndMessage(
            String price, String stock, String field, String message) {
        knownCategories();
        persistAllAsInserted();
        String csv = HEADER + row("ELEC1", "Electronics", price, stock) + row("ELEC2", "Electronics", "1.00", "1");

        CsvImportReport report = run(service(10, 10), csv);

        assertThat(report.totalRows()).isEqualTo(2);
        assertThat(report.insertedRows()).isEqualTo(1);
        assertThat(report.rejectedRows()).isEqualTo(1);
        assertThat(report.errors()).hasSize(1);
        CsvRowError error = report.errors().getFirst();
        assertThat(error.field()).isEqualTo(field);
        assertThat(error.message()).contains(message);
        assertThat(error.line()).isEqualTo(2);
        assertThat(error.sku()).isEqualTo("ELEC1");
    }

    @ParameterizedTest
    @ValueSource(strings = {"elec-1", "elec1", "TOOLONGSKU1X"})
    void importCsv_invalidSku_isRejected(String sku) {
        knownCategories();
        persistAllAsInserted();
        String csv = HEADER + row(sku, "Electronics", "1.00", "1") + row("OK1", "Electronics", "1.00", "1");

        CsvImportReport report = run(service(10, 10), csv);

        assertThat(report.rejectedRows()).isEqualTo(1);
        assertThat(report.errors()).extracting(CsvRowError::field).contains("sku");
        assertThat(batches.getFirst()).extracting(ProductUpsertRow::sku).containsExactly("OK1");
    }

    @Test
    void importCsv_unknownCategory_isRejectedListingAllowedValues() {
        knownCategories();
        persistAllAsInserted();
        String csv = HEADER + row("TOY1", "Toys", "1.00", "1") + row("ELEC1", "Electronics", "1.00", "1");

        CsvImportReport report = run(service(10, 10), csv);

        assertThat(report.rejectedRows()).isEqualTo(1);
        CsvRowError error = report.errors().getFirst();
        assertThat(error.field()).isEqualTo("category");
        assertThat(error.rejectedValue()).isEqualTo("Toys");
        assertThat(error.message()).contains("Books, Electronics");
    }

    @Test
    void importCsv_missingRequiredValue_isReportedOnItsField() {
        knownCategories();
        persistAllAsInserted();
        String csv = HEADER + ",desc,ELEC1,Electronics,1.00,1,\n" + row("ELEC2", "Electronics", "1.00", "1");

        CsvImportReport report = run(service(10, 10), csv);

        assertThat(report.errors()).extracting(CsvRowError::field).containsExactly("name");
        assertThat(report.insertedRows()).isEqualTo(1);
    }

    @Test
    void importCsv_duplicateSkuInFile_firstOccurrenceWins() {
        knownCategories();
        persistAllAsInserted();
        String csv = HEADER + row("DUP1", "Electronics", "1.00", "1") + row("DUP1", "Books", "2.00", "2");

        CsvImportReport report = run(service(10, 10), csv);

        assertThat(report.insertedRows()).isEqualTo(1);
        assertThat(report.rejectedRows()).isEqualTo(1);
        assertThat(batches.getFirst()).hasSize(1);
        assertThat(batches.getFirst().getFirst().categoryId()).isEqualTo((short) 1);
        CsvRowError error = report.errors().getFirst();
        assertThat(error.field()).isEqualTo("sku");
        assertThat(error.line()).isEqualTo(3);
        assertThat(error.message()).contains("first occurrence at line 2");
    }

    @Test
    void importCsv_rowWithMissingColumns_isRejectedAsInconsistent() {
        knownCategories();
        persistAllAsInserted();
        String csv = HEADER + "Mouse,desc,ELEC1\n" + row("ELEC2", "Electronics", "1.00", "1");

        CsvImportReport report = run(service(10, 10), csv);

        assertThat(report.rejectedRows()).isEqualTo(1);
        assertThat(report.errors().getFirst().message()).contains("Row has 3 column(s)");
        assertThat(report.insertedRows()).isEqualTo(1);
    }

    @Test
    void importCsv_batchSizeTwo_splitsRowsSortedBySkuAndSumsOutcomes() {
        knownCategories();
        given(batchRepository.upsertBatch(anyList())).willAnswer(invocation -> {
            List<ProductUpsertRow> rows = invocation.getArgument(0);
            batches.add(rows);
            return new UpsertOutcome(rows.size() - 1, 1);
        });
        String csv = HEADER + row("E", "Electronics", "1.00", "1") + row("C", "Electronics", "1.00", "1")
                + row("D", "Electronics", "1.00", "1") + row("A", "Electronics", "1.00", "1")
                + row("B", "Electronics", "1.00", "1");

        CsvImportReport report = run(service(2, 10), csv);

        verify(batchRepository, times(3)).upsertBatch(anyList());
        List<List<String>> skusPerBatch = batches.stream()
                .map(rows -> rows.stream().map(ProductUpsertRow::sku).collect(Collectors.toList()))
                .sorted(Comparator.comparing(list -> list.getFirst()))
                .toList();
        assertThat(skusPerBatch).containsExactly(List.of("A", "D"), List.of("B"), List.of("C", "E"));
        assertThat(report.totalRows()).isEqualTo(5);
        assertThat(report.insertedRows()).isEqualTo(2);
        assertThat(report.updatedRows()).isEqualTo(3);
        assertThat(report.acceptedRows()).isEqualTo(5);
        assertThat(report.aborted()).isFalse();
    }

    @Test
    void importCsv_rowsWithinEachBatch_areSortedBySku() {
        knownCategories();
        persistAllAsInserted();
        String csv = HEADER + row("C", "Electronics", "1.00", "1") + row("A", "Electronics", "1.00", "1")
                + row("B", "Electronics", "1.00", "1");

        run(service(3, 10), csv);

        assertThat(batches).hasSize(1);
        assertThat(batches.getFirst()).extracting(ProductUpsertRow::sku).containsExactly("A", "B", "C");
    }

    @Test
    void importCsv_oneBatchFails_itsRowsAreRejectedAndTheRestContinues() {
        knownCategories();
        given(batchRepository.upsertBatch(anyList())).willAnswer(invocation -> {
            List<ProductUpsertRow> rows = invocation.getArgument(0);
            if (rows.stream().anyMatch(row -> row.sku().equals("A"))) {
                throw new IllegalStateException("deadlock detected");
            }
            return new UpsertOutcome(rows.size(), 0);
        });
        String csv = HEADER + row("A", "Electronics", "1.00", "1") + row("B", "Electronics", "1.00", "1")
                + row("C", "Electronics", "1.00", "1") + row("D", "Electronics", "1.00", "1");

        CsvImportReport report = run(service(2, 10), csv);

        assertThat(report.insertedRows()).isEqualTo(2);
        assertThat(report.rejectedRows()).isEqualTo(2);
        assertThat(report.errors()).extracting(CsvRowError::sku).containsExactlyInAnyOrder("A", "B");
        assertThat(report.errors()).allSatisfy(error ->
                assertThat(error.message()).contains("Batch persistence failed").contains("deadlock detected"));
    }

    @Test
    void importCsv_moreErrorsThanLimit_truncatesDetailsButCountsAllRejectedRows() {
        knownCategories();
        persistAllAsInserted();
        StringBuilder csv = new StringBuilder(HEADER).append(row("OK1", "Electronics", "1.00", "1"));
        for (int i = 0; i < 5; i++) {
            csv.append(row("BAD" + i, "Toys", "1.00", "1"));
        }

        CsvImportReport report = run(service(10, 2), csv.toString());

        assertThat(report.errorsTruncated()).isTrue();
        assertThat(report.rejectedRows()).isEqualTo(5);
        assertThat(report.errors()).hasSize(2);
        assertThat(report.insertedRows()).isEqualTo(1);
    }

    @Test
    void importCsv_errorsWithinLimit_areNotTruncated() {
        knownCategories();
        persistAllAsInserted();

        CsvImportReport report = run(service(10, 5),
                HEADER + row("OK1", "Electronics", "1.00", "1") + row("BAD", "Toys", "1.00", "1"));

        assertThat(report.errorsTruncated()).isFalse();
    }

    @Test
    void importCsv_headerOnly_throwsNoDataRow() {
        knownCategories();

        assertThatThrownBy(() -> run(service(10, 10), HEADER))
                .isInstanceOf(InvalidCsvFileException.class).hasMessageContaining("does not contain any data row");
    }

    @Test
    void importCsv_allRowsInvalid_throwsRejectedWithReport() {
        knownCategories();
        String csv = HEADER + row("A", "Toys", "1.00", "1") + row("B", "Toys", "x", "1");

        assertThatThrownBy(() -> run(service(10, 10), csv))
                .isInstanceOfSatisfying(CsvIngestionRejectedException.class, exception -> {
                    assertThat(exception.getReport().totalRows()).isEqualTo(2);
                    assertThat(exception.getReport().rejectedRows()).isEqualTo(2);
                    assertThat(exception.getReport().acceptedRows()).isZero();
                });
        verify(batchRepository, never()).upsertBatch(anyList());
    }

    @Test
    void importCsv_unclosedQuoteMidFile_abortsKeepingProcessedRows() {
        knownCategories();
        persistAllAsInserted();
        String csv = HEADER + row("OK1", "Electronics", "1.00", "1") + row("OK2", "Electronics", "1.00", "1")
                + "\"Broken,desc,BAD1,Electronics,1.00,1,0.5\n";

        CsvImportReport report = run(service(10, 10), csv);

        assertThat(report.aborted()).isTrue();
        assertThat(report.insertedRows()).isEqualTo(2);
        assertThat(report.errors()).anySatisfy(error ->
                assertThat(error.message()).contains("Malformed CSV content, ingestion aborted"));
    }

    @Test
    void importCsv_unclosedQuoteBeforeAnyRow_throwsMalformed() {
        knownCategories();
        String csv = HEADER + "\"Broken,desc,BAD1,Electronics,1.00,1,0.5\n";

        assertThatThrownBy(() -> run(service(10, 10), csv))
                .isInstanceOf(InvalidCsvFileException.class);
    }
}
