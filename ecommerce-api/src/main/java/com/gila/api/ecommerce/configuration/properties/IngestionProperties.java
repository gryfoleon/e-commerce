package com.gila.api.ecommerce.configuration.properties;

import java.time.Duration;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.ingestion")
public record IngestionProperties(
        Integer batchSize,
        Integer maxReportedErrors,
        Integer maxConcurrentBatches,
        Duration idempotencyLockTtl) {

    public IngestionProperties {
        batchSize = Math.max(1, Objects.requireNonNullElse(batchSize, 1000));
        maxReportedErrors = Math.max(1, Objects.requireNonNullElse(maxReportedErrors, 1000));
        maxConcurrentBatches = Math.max(1, Objects.requireNonNullElse(maxConcurrentBatches, 4));
        idempotencyLockTtl = Objects.requireNonNullElse(idempotencyLockTtl, Duration.ofMinutes(10));
    }
}
