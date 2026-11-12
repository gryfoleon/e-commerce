package com.gila.api.ecommerce.configuration.properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ProductCacheAndIngestionPropertiesTest {

    @Test
    void productCacheProperties_noValues_usesDefaults() {
        ProductCacheProperties properties = new ProductCacheProperties(null, null, null, null);

        assertThat(properties.enabled()).isTrue();
        assertThat(properties.keyPrefix()).isEqualTo("product:v1:");
        assertThat(properties.detailTtl()).isEqualTo(Duration.ofHours(6));
        assertThat(properties.queryTtl()).isEqualTo(Duration.ofSeconds(30));
    }

    @ParameterizedTest
    @ValueSource(longs = {2, 6, 12})
    void productCacheProperties_detailTtlWithinBounds_isAccepted(long hours) {
        assertThat(new ProductCacheProperties(true, null, Duration.ofHours(hours), null).detailTtl())
                .isEqualTo(Duration.ofHours(hours));
    }

    @ParameterizedTest
    @ValueSource(longs = {0, 60, 119, 721, 24 * 60})
    void productCacheProperties_detailTtlOutsideTwoToTwelveHours_failsFast(long minutes) {
        assertThatThrownBy(() -> new ProductCacheProperties(true, null, Duration.ofMinutes(minutes), null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("detail-ttl");
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1})
    void productCacheProperties_nonPositiveQueryTtl_failsFast(long seconds) {
        assertThatThrownBy(() -> new ProductCacheProperties(true, null, null, Duration.ofSeconds(seconds)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("query-ttl");
    }

    @Test
    void productCacheProperties_disabledFlag_isKept() {
        assertThat(new ProductCacheProperties(false, null, null, null).enabled()).isFalse();
    }

    @Test
    void ingestionProperties_noValues_usesDefaults() {
        IngestionProperties properties = new IngestionProperties(null, null, null, null);

        assertThat(properties.batchSize()).isEqualTo(1000);
        assertThat(properties.maxReportedErrors()).isEqualTo(1000);
        assertThat(properties.maxConcurrentBatches()).isEqualTo(4);
        assertThat(properties.idempotencyLockTtl()).isEqualTo(Duration.ofMinutes(10));
    }

    @Test
    void ingestionProperties_nonPositiveValues_areClampedToOne() {
        IngestionProperties properties = new IngestionProperties(0, -3, 0, Duration.ofSeconds(5));

        assertThat(properties.batchSize()).isEqualTo(1);
        assertThat(properties.maxReportedErrors()).isEqualTo(1);
        assertThat(properties.maxConcurrentBatches()).isEqualTo(1);
        assertThat(properties.idempotencyLockTtl()).isEqualTo(Duration.ofSeconds(5));
    }

    @Test
    void idempotencyProperties_noValues_usesDefaults() {
        IdempotencyProperties properties = new IdempotencyProperties(null, null, null);

        assertThat(properties.keyPrefix()).isEqualTo("idempotency-key:");
        assertThat(properties.lockTtl()).isEqualTo(Duration.ofSeconds(30));
        assertThat(properties.recordTtl()).isEqualTo(Duration.ofHours(24));
    }
}
