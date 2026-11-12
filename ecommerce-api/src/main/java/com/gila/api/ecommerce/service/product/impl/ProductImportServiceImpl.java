package com.gila.api.ecommerce.service.product.impl;

import com.gila.api.ecommerce.configuration.properties.IngestionProperties;
import com.gila.api.ecommerce.error.exceptions.CsvIngestionRejectedException;
import com.gila.api.ecommerce.error.exceptions.InvalidCsvFileException;
import com.gila.api.ecommerce.model.dto.product.CsvImportReport;
import com.gila.api.ecommerce.model.dto.product.CsvRowError;
import com.gila.api.ecommerce.model.dto.product.CsvUploadMetadata;
import com.gila.api.ecommerce.model.dto.product.ProductCreateRequest;
import com.gila.api.ecommerce.model.query.ProductUpsertRow;
import com.gila.api.ecommerce.model.query.UpsertOutcome;
import com.gila.api.ecommerce.repository.product.ProductBatchRepository;
import com.gila.api.ecommerce.service.category.CategoryService;
import com.gila.api.ecommerce.service.product.ProductImportService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.input.BOMInputStream;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ProductImportServiceImpl implements ProductImportService {

    private static final Logger logger = LoggerFactory.getLogger(ProductImportServiceImpl.class);

    static final String COLUMN_NAME = "name";
    static final String COLUMN_DESCRIPTION = "description";
    static final String COLUMN_SKU = "sku";
    static final String COLUMN_CATEGORY = "category";
    static final String COLUMN_PRICE = "price";
    static final String COLUMN_STOCK = "stock";
    static final String COLUMN_WEIGHT_KG = "weight_kg";
    static final List<String> REQUIRED_COLUMNS =
            List.of(COLUMN_NAME, COLUMN_SKU, COLUMN_CATEGORY, COLUMN_PRICE, COLUMN_STOCK);

    private static final Set<String> ACCEPTED_CONTENT_TYPES = Set.of(
            "text/csv", "application/csv", "text/plain", "application/vnd.ms-excel", "application/octet-stream");
    private static final Map<String, String> FIELD_BY_PROPERTY = Map.of(
            "sku", COLUMN_SKU, "name", COLUMN_NAME, "description", COLUMN_DESCRIPTION,
            "category", COLUMN_CATEGORY, "price", COLUMN_PRICE, "stock", COLUMN_STOCK, "weightKg", COLUMN_WEIGHT_KG);
    private static final CSVFormat CSV_FORMAT = CSVFormat.DEFAULT.builder()
            .setHeader()
            .setSkipHeaderRecord(true)
            .setIgnoreHeaderCase(true)
            .setIgnoreEmptyLines(true)
            .setTrim(true)
            .get();

    private final ProductBatchRepository batchRepository;
    private final CategoryService categoryService;
    private final Validator validator;
    private final IngestionProperties properties;

    public ProductImportServiceImpl(
            ProductBatchRepository batchRepository,
            CategoryService categoryService,
            Validator validator,
            IngestionProperties properties) {
        this.batchRepository = batchRepository;
        this.categoryService = categoryService;
        this.validator = validator;
        this.properties = properties;
    }

    @Override
    public CsvImportReport importCsv(CsvUploadMetadata metadata, InputStream input) {
        validateMetadata(metadata);
        long startedAt = System.nanoTime();
        ErrorCollector errors = new ErrorCollector(properties.maxReportedErrors());
        try (BOMInputStream bomInput = BOMInputStream.builder().setInputStream(input).get();
             BufferedReader reader = new BufferedReader(new InputStreamReader(bomInput, StandardCharsets.UTF_8));
             CSVParser parser = CSV_FORMAT.parse(reader);
             ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            validateHeader(parser.getHeaderNames());
            IngestionRun run = new IngestionRun(executor, errors);
            boolean aborted = run.consume(parser.iterator());
            UpsertOutcome outcome = run.awaitCompletion();
            CsvImportReport report = new CsvImportReport(run.totalRows, outcome.inserted(), outcome.updated(),
                    errors.rejectedRows(), errors.truncated(), aborted,
                    (System.nanoTime() - startedAt) / 1_000_000, errors.snapshot());
            return finish(metadata, report);
        } catch (IOException | UncheckedIOException exception) {
            throw new InvalidCsvFileException("The CSV file could not be read: " + rootMessage(exception), exception);
        }
    }

    private CsvImportReport finish(CsvUploadMetadata metadata, CsvImportReport report) {
        logger.info("CSV import file={} total={} inserted={} updated={} rejected={} aborted={} durationMs={}",
                metadata.filename(), report.totalRows(), report.insertedRows(), report.updatedRows(),
                report.rejectedRows(), report.aborted(), report.durationMs());
        if (report.totalRows() == 0) {
            if (report.aborted()) {
                throw new InvalidCsvFileException("The CSV file is malformed: " + report.errors().getFirst().message());
            }
            throw new InvalidCsvFileException("The CSV file does not contain any data row.");
        }
        if (report.acceptedRows() == 0) {
            throw new CsvIngestionRejectedException(report);
        }
        return report;
    }

    private static void validateMetadata(CsvUploadMetadata metadata) {
        if (metadata.size() <= 0) {
            throw new InvalidCsvFileException("The uploaded file is empty.");
        }
        String filename = Optional.ofNullable(metadata.filename()).orElse("").toLowerCase(Locale.ROOT);
        String contentType = Optional.ofNullable(metadata.contentType())
                .map(type -> type.split(";")[0].strip().toLowerCase(Locale.ROOT))
                .orElse("");
        if (!filename.endsWith(".csv") && !ACCEPTED_CONTENT_TYPES.contains(contentType)) {
            throw new InvalidCsvFileException(
                    "Only CSV files are accepted (extension .csv or content type text/csv).");
        }
    }

    private static void validateHeader(List<String> headerNames) {
        Set<String> present = headerNames.stream()
                .map(header -> header.strip().toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        List<String> missing = REQUIRED_COLUMNS.stream().filter(column -> !present.contains(column)).toList();
        if (!missing.isEmpty()) {
            throw new InvalidCsvFileException("The CSV header is missing required column(s): "
                    + String.join(", ", missing) + ". Expected: " + String.join(",", REQUIRED_COLUMNS)
                    + " plus optional " + COLUMN_DESCRIPTION + "," + COLUMN_WEIGHT_KG + ".");
        }
    }

    private static String rootMessage(Throwable throwable) {
        Throwable root = throwable;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return Optional.ofNullable(root.getMessage()).orElse(root.getClass().getSimpleName());
    }

    private final class IngestionRun {

        private final ExecutorService executor;
        private final ErrorCollector errors;
        private final Semaphore permits = new Semaphore(properties.maxConcurrentBatches());
        private final List<Future<UpsertOutcome>> futures = new ArrayList<>();
        private final Map<String, Long> firstLineBySku = new HashMap<>();
        private List<ProductUpsertRow> batch = new ArrayList<>(properties.batchSize());
        private final Map<String, Short> idByName = categoryService.findIdsByName();
        private final List<String> categoryNames = idByName.keySet().stream().sorted().toList();
        private final Map<String, Short> idByLowerName = idByName.entrySet().stream()
                .collect(Collectors.toMap(entry -> entry.getKey().toLowerCase(Locale.ROOT), Map.Entry::getValue));
        private long totalRows;

        private IngestionRun(ExecutorService executor, ErrorCollector errors) {
            this.executor = executor;
            this.errors = errors;
        }

        private boolean consume(Iterator<CSVRecord> records) {
            try {
                while (records.hasNext()) {
                    CSVRecord record = records.next();
                    totalRows++;
                    toUpsertRow(record).ifPresent(this::append);
                }
                flush();
                return false;
            } catch (UncheckedIOException | IllegalStateException exception) {
                long line = totalRows + 2;
                logger.warn("CSV parsing aborted near line {}: {}", line, rootMessage(exception));
                errors.add(new CsvRowError(line, null, null, null, "Malformed CSV content, ingestion aborted: "
                        + rootMessage(exception)), false);
                flush();
                return true;
            }
        }

        private Optional<ProductUpsertRow> toUpsertRow(CSVRecord record) {
            long line = record.getRecordNumber() + 1;
            String sku = value(record, COLUMN_SKU);
            if (!record.isConsistent()) {
                errors.add(new CsvRowError(line, sku, null, null,
                        "Row has " + record.size() + " column(s) but the header defines "
                                + record.getParser().getHeaderNames().size() + "."), true);
                return Optional.empty();
            }
            List<CsvRowError> rowErrors = new ArrayList<>();
            BigDecimal price = parseDecimal(record, COLUMN_PRICE, line, sku, rowErrors);
            Integer stock = parseInteger(record, COLUMN_STOCK, line, sku, rowErrors);
            BigDecimal weightKg = parseDecimal(record, COLUMN_WEIGHT_KG, line, sku, rowErrors);
            ProductCreateRequest request = new ProductCreateRequest(sku, value(record, COLUMN_NAME),
                    value(record, COLUMN_DESCRIPTION), value(record, COLUMN_CATEGORY), price, stock, weightKg);
            Set<String> parseFailures = rowErrors.stream().map(CsvRowError::field).collect(Collectors.toSet());
            validator.validate(request).stream()
                    .sorted(Comparator.comparing(violation -> violation.getPropertyPath().toString()))
                    .map(violation -> toRowError(violation, line, request.sku()))
                    .filter(error -> !parseFailures.contains(error.field()))
                    .forEach(rowErrors::add);
            Optional<Short> categoryId = Optional.ofNullable(request.category())
                    .filter(name -> !name.isBlank())
                    .map(name -> idByLowerName.get(name.strip().toLowerCase(Locale.ROOT)));
            if (categoryId.isEmpty() && !parseFailures.contains(COLUMN_CATEGORY) && request.category() != null
                    && !request.category().isBlank()) {
                rowErrors.add(new CsvRowError(line, request.sku(), COLUMN_CATEGORY, request.category(),
                        "Unknown category. Allowed values: " + String.join(", ", categoryNames)));
            }
            if (rowErrors.isEmpty() && request.sku() != null) {
                Long firstLine = firstLineBySku.putIfAbsent(request.sku(), line);
                if (firstLine != null) {
                    rowErrors.add(new CsvRowError(line, request.sku(), COLUMN_SKU, request.sku(),
                            "Duplicate SKU in file, first occurrence at line " + firstLine + "."));
                }
            }
            if (!rowErrors.isEmpty()) {
                errors.addAll(rowErrors);
                return Optional.empty();
            }
            return Optional.of(new ProductUpsertRow(line, request.sku(), request.name(), request.description(),
                    categoryId.orElseThrow(), request.price(), request.stock(), request.weightKg()));
        }

        private void append(ProductUpsertRow row) {
            batch.add(row);
            if (batch.size() >= properties.batchSize()) {
                flush();
            }
        }

        private void flush() {
            if (batch.isEmpty()) {
                return;
            }
            List<ProductUpsertRow> rows = batch.stream().sorted(Comparator.comparing(ProductUpsertRow::sku)).toList();
            batch = new ArrayList<>(properties.batchSize());
            permits.acquireUninterruptibly();
            futures.add(executor.submit(() -> {
                try {
                    return batchRepository.upsertBatch(rows);
                } catch (RuntimeException exception) {
                    logger.error("CSV batch of {} row(s) failed", rows.size(), exception);
                    rows.forEach(row -> errors.add(new CsvRowError(row.line(), row.sku(), null, null,
                            "Batch persistence failed: " + rootMessage(exception)), true));
                    return UpsertOutcome.EMPTY;
                } finally {
                    permits.release();
                }
            }));
        }

        private UpsertOutcome awaitCompletion() {
            UpsertOutcome total = UpsertOutcome.EMPTY;
            for (Future<UpsertOutcome> future : futures) {
                try {
                    total = total.plus(future.get());
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("CSV ingestion was interrupted", exception);
                } catch (ExecutionException exception) {
                    throw new IllegalStateException("CSV ingestion batch failed", exception.getCause());
                }
            }
            return total;
        }
    }

    private static CsvRowError toRowError(ConstraintViolation<ProductCreateRequest> violation, long line,
                                          @Nullable String sku) {
        String property = violation.getPropertyPath().toString();
        Object invalid = violation.getInvalidValue();
        return new CsvRowError(line, sku, FIELD_BY_PROPERTY.getOrDefault(property, property),
                invalid != null ? invalid.toString() : null, violation.getMessage());
    }

    private static @Nullable String value(CSVRecord record, String column) {
        if (!record.isMapped(column) || !record.isSet(column)) {
            return null;
        }
        String value = record.get(column);
        return value == null || value.isBlank() ? null : value;
    }

    private static @Nullable BigDecimal parseDecimal(CSVRecord record, String column, long line,
                                                     @Nullable String sku, List<CsvRowError> rowErrors) {
        String raw = value(record, column);
        if (raw == null) {
            return null;
        }
        try {
            return new BigDecimal(raw);
        } catch (NumberFormatException exception) {
            rowErrors.add(new CsvRowError(line, sku, column, raw, "Value must be a decimal number."));
            return null;
        }
    }

    private static @Nullable Integer parseInteger(CSVRecord record, String column, long line,
                                                  @Nullable String sku, List<CsvRowError> rowErrors) {
        String raw = value(record, column);
        if (raw == null) {
            return null;
        }
        try {
            return Integer.valueOf(raw);
        } catch (NumberFormatException exception) {
            rowErrors.add(new CsvRowError(line, sku, column, raw, "Value must be an integer number."));
            return null;
        }
    }

    private static final class ErrorCollector {

        private final int maxErrors;
        private final List<CsvRowError> errors = new ArrayList<>();
        private final AtomicLong rejectedRows = new AtomicLong();
        private volatile boolean truncated;

        private ErrorCollector(int maxErrors) {
            this.maxErrors = maxErrors;
        }

        private void add(CsvRowError error, boolean countsAsRejectedRow) {
            if (countsAsRejectedRow) {
                rejectedRows.incrementAndGet();
            }
            append(error);
        }

        private void addAll(List<CsvRowError> rowErrors) {
            rejectedRows.incrementAndGet();
            rowErrors.forEach(this::append);
        }

        private synchronized void append(CsvRowError error) {
            if (errors.size() < maxErrors) {
                errors.add(error);
            } else {
                truncated = true;
            }
        }

        private long rejectedRows() {
            return rejectedRows.get();
        }

        private boolean truncated() {
            return truncated;
        }

        private synchronized List<CsvRowError> snapshot() {
            return errors.stream().sorted(Comparator.comparingLong(CsvRowError::line)).toList();
        }
    }
}
