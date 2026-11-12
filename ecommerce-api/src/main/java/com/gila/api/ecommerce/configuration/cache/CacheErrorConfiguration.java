package com.gila.api.ecommerce.configuration.cache;

import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class CacheErrorConfiguration implements CachingConfigurer {

    private final ProductCacheErrorHandler errorHandler;

    public CacheErrorConfiguration(ProductCacheErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return errorHandler;
    }
}
