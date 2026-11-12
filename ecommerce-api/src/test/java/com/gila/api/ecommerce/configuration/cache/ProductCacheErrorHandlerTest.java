package com.gila.api.ecommerce.configuration.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.gila.api.ecommerce.configuration.properties.ProductCacheProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;

class ProductCacheErrorHandlerTest {

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final RuntimeException error = new RuntimeException("redis down");
    private ProductCacheErrorHandler handler;
    private Cache cache;

    @BeforeEach
    void setUp() {
        handler = new ProductCacheErrorHandler(registry, new ProductCacheProperties(true, null, null, null));
        cache = mock(Cache.class);
        when(cache.getName()).thenReturn(CacheNames.PRODUCT_DETAIL);
    }

    private double count(String operation) {
        return registry.counter("catalog.cache.error", "cache", CacheNames.PRODUCT_DETAIL, "operation", operation)
                .count();
    }

    @Test
    void handleCacheGetError_failsOpenAndCountsGet() {
        handler.handleCacheGetError(error, cache, "k");

        assertThat(count("get")).isEqualTo(1.0);
    }

    @Test
    void handleCachePutError_failsOpenAndCountsPut() {
        handler.handleCachePutError(error, cache, "k", "v");

        assertThat(count("put")).isEqualTo(1.0);
    }

    @Test
    void handleCacheEvictError_doesNotThrowAndCountsEvict() {
        handler.handleCacheEvictError(error, cache, "k");

        assertThat(count("evict")).isEqualTo(1.0);
    }

    @Test
    void handleCacheClearError_doesNotThrowAndCountsClear() {
        handler.handleCacheClearError(error, cache);

        assertThat(count("clear")).isEqualTo(1.0);
    }

    @Test
    void handleCacheGetError_repeatedFailures_accumulateCounter() {
        handler.handleCacheGetError(error, cache, "a");
        handler.handleCacheGetError(error, cache, "b");

        assertThat(count("get")).isEqualTo(2.0);
        assertThat(count("put")).isZero();
    }

    @Test
    void handleCacheEvictError_queryCache_usesItsOwnCounterTag() {
        Cache query = mock(Cache.class);
        when(query.getName()).thenReturn(CacheNames.PRODUCT_QUERY);

        handler.handleCacheEvictError(error, query, "k");

        assertThat(registry.counter("catalog.cache.error", "cache", CacheNames.PRODUCT_QUERY, "operation", "evict")
                .count()).isEqualTo(1.0);
        assertThat(count("evict")).isZero();
    }
}
