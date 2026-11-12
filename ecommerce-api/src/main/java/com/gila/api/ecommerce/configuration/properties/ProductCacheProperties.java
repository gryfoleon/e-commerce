package com.gila.api.ecommerce.configuration.properties;

import java.time.Duration;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.cache")
public record ProductCacheProperties(Boolean enabled, String keyPrefix, Duration detailTtl, Duration queryTtl) {

    static final Duration MIN_DETAIL_TTL = Duration.ofHours(2);
    static final Duration MAX_DETAIL_TTL = Duration.ofHours(12);

    public ProductCacheProperties {
        enabled = Objects.requireNonNullElse(enabled, Boolean.TRUE);
        keyPrefix = Objects.requireNonNullElse(keyPrefix, "product:v1:");
        detailTtl = Objects.requireNonNullElse(detailTtl, Duration.ofHours(6));
        if (detailTtl.compareTo(MIN_DETAIL_TTL) < 0 || detailTtl.compareTo(MAX_DETAIL_TTL) > 0) {
            throw new IllegalArgumentException("app.cache.detail-ttl must be between 2h and 12h");
        }
        queryTtl = Objects.requireNonNullElse(queryTtl, Duration.ofSeconds(30));
        if (queryTtl.isNegative() || queryTtl.isZero()) {
            throw new IllegalArgumentException("app.cache.query-ttl must be a positive duration");
        }
    }
}
