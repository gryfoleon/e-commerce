package com.gila.api.ecommerce.service.product;

import com.gila.api.ecommerce.model.dto.product.CsvImportReport;
import com.gila.api.ecommerce.model.dto.product.CsvUploadMetadata;
import java.io.InputStream;

public interface ProductImportService {

    CsvImportReport importCsv(CsvUploadMetadata metadata, InputStream input);
}
