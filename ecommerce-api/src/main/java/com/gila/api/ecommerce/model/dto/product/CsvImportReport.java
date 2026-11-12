package com.gila.api.ecommerce.model.dto.product;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;

@Schema(description = "CSV ingestion report")
@JsonPropertyOrder({"totalRows", "insertedRows", "updatedRows", "rejectedRows", "errorsTruncated", "aborted",
        "durationMs", "errors"})
public record CsvImportReport(
        long totalRows,
        long insertedRows,
        long updatedRows,
        long rejectedRows,
        boolean errorsTruncated,
        boolean aborted,
        long durationMs,
        List<CsvRowError> errors) {

    public CsvImportReport {
        errors = List.copyOf(Objects.requireNonNullElseGet(errors, List::of));
    }

    public long acceptedRows() {
        return insertedRows + updatedRows;
    }
}
