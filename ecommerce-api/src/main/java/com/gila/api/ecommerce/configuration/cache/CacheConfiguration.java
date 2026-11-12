package com.gila.api.ecommerce.configuration.cache;

import com.gila.api.ecommerce.configuration.properties.ProductCacheProperties;
import com.gila.api.ecommerce.model.dto.PageableResponse;
import com.gila.api.ecommerce.model.dto.category.CategoryResponse;
import com.gila.api.ecommerce.model.dto.product.ProductResponse;
import com.gila.api.ecommerce.model.dto.product.ProductSearchDTO;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.data.redis.cache.BatchStrategies;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.json.JsonMapper;

@Configuration(proxyBeanMethods = false)
@EnableCaching(order = Ordered.HIGHEST_PRECEDENCE)
public class CacheConfiguration {

    private static final int CLEAR_SCAN_BATCH_SIZE = 1000;

    @Bean
    CacheManager cacheManager(
            RedisConnectionFactory connectionFactory, ProductCacheProperties properties, JsonMapper jsonMapper) {
        if (!properties.enabled()) {
            return new NoOpCacheManager();
        }
        JavaType pageType = jsonMapper.getTypeFactory()
                .constructParametricType(PageableResponse.class, ProductSearchDTO.class);
        JavaType categoryPageType = jsonMapper.getTypeFactory()
                .constructParametricType(PageableResponse.class, CategoryResponse.class);
        JavaType namesType = jsonMapper.getTypeFactory().constructCollectionType(List.class, String.class);
        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues()
                .prefixCacheNameWith(properties.keyPrefix())
                .serializeKeysWith(SerializationPair.fromSerializer(new StringRedisSerializer()));
        Map<String, RedisCacheConfiguration> regions = Map.of(
                CacheNames.PRODUCT_DETAIL, region(defaults, properties.detailTtl(),
                        new JacksonJsonRedisSerializer<>(jsonMapper, ProductResponse.class)),
                CacheNames.PRODUCT_QUERY, region(defaults, properties.queryTtl(),
                        new JacksonJsonRedisSerializer<>(jsonMapper, pageType)),
                CacheNames.CATEGORY_DETAIL, region(defaults, properties.detailTtl(),
                        new JacksonJsonRedisSerializer<>(jsonMapper, CategoryResponse.class)),
                CacheNames.CATEGORY_NAME, region(defaults, properties.detailTtl(),
                        new JacksonJsonRedisSerializer<>(jsonMapper, CategoryResponse.class)),
                CacheNames.CATEGORY_LIST, region(defaults, properties.detailTtl(),
                        new JacksonJsonRedisSerializer<>(jsonMapper, categoryPageType)),
                CacheNames.CATEGORY_NAMES, region(defaults, properties.detailTtl(),
                        new JacksonJsonRedisSerializer<>(jsonMapper, namesType)));
        return RedisCacheManager.builder(RedisCacheWriter.nonLockingRedisCacheWriter(
                        connectionFactory, BatchStrategies.scan(CLEAR_SCAN_BATCH_SIZE)))
                .cacheDefaults(defaults)
                .withInitialCacheConfigurations(regions)
                .disableCreateOnMissingCache()
                .enableStatistics()
                .build();
    }

    private static RedisCacheConfiguration region(
            RedisCacheConfiguration defaults, Duration ttl, JacksonJsonRedisSerializer<?> serializer) {
        return defaults.entryTtl(ttl).serializeValuesWith(SerializationPair.fromSerializer(serializer));
    }
}
