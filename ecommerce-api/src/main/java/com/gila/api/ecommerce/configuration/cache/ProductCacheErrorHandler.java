package com.gila.api.ecommerce.configuration.cache;

import com.gila.api.ecommerce.configuration.properties.ProductCacheProperties;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ProductCacheErrorHandler implements CacheErrorHandler {

    private static final String METRIC = "catalog.cache.error";

    private final MeterRegistry meterRegistry;
    private final ProductCacheProperties properties;

    public ProductCacheErrorHandler(MeterRegistry meterRegistry, ProductCacheProperties properties) {
        this.meterRegistry = meterRegistry;
        this.properties = properties;
    }

    @Override
    public void handleCacheGetError(RuntimeException exception, @NonNull Cache cache, @NonNull Object key) {
        record(cache, "get");
        log.warn("Cache read failed in {} for key {}, falling back to the database: {}",
                cache.getName(), key, exception.toString());
    }

    @Override
    public void handleCachePutError(RuntimeException exception, @NonNull Cache cache, @NonNull Object key, @Nullable Object value) {
        record(cache, "put");
        log.warn("Cache write failed in {} for key {}: {}", cache.getName(), key, exception.toString());
    }

    @Override
    public void handleCacheEvictError(@NonNull RuntimeException exception, @NonNull Cache cache, @NonNull Object key) {
        record(cache, "evict");
        log.error("Cache eviction failed in {} for key {}; the entry may be served stale for up to {}",
                cache.getName(), key, maxStaleness(cache), exception);
    }

    @Override
    public void handleCacheClearError(@NonNull RuntimeException exception, @NonNull Cache cache) {
        record(cache, "clear");
        log.error("Cache clear failed in {}; entries may be served stale for up to {}",
                cache.getName(), maxStaleness(cache), exception);
    }

    private void record(Cache cache, String operation) {
        meterRegistry.counter(METRIC, "cache", cache.getName(), "operation", operation).increment();
    }

    private Object maxStaleness(Cache cache) {
        return CacheNames.PRODUCT_DETAIL.equals(cache.getName()) ? properties.detailTtl() : properties.queryTtl();
    }
}
