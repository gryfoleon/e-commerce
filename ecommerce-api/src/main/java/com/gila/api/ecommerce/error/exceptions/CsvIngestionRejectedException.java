package com.gila.api.ecommerce.error.exceptions;

import com.gila.api.ecommerce.model.dto.product.CsvImportReport;

public final class CsvIngestionRejectedException extends RuntimeException {

    private final transient CsvImportReport report;

    public CsvIngestionRejectedException(CsvImportReport report) {
        super("The CSV file does not contain any valid product row. "
                + report.rejectedRows() + " row(s) were rejected.");
        this.report = report;
    }

    public CsvImportReport getReport() {
        return report;
    }
}
