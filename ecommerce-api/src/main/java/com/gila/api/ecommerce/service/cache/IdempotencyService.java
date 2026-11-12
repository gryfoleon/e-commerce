package com.gila.api.ecommerce.service.cache;

import com.gila.api.ecommerce.configuration.properties.IdempotencyProperties;
import com.gila.api.ecommerce.error.exceptions.ConcurrentIdempotencyException;
import com.gila.api.ecommerce.error.exceptions.IdempotencyKeyMismatchException;
import com.gila.api.ecommerce.model.cache.IdempotencyExecutionResult;
import com.gila.api.ecommerce.model.cache.IdempotentResponseRecord;
import java.time.Duration;
import java.util.function.Supplier;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Service
public class IdempotencyService {

    private static final String IN_PROGRESS_PREFIX = "IN_PROGRESS:";

    private final StringRedisTemplate stringRedisTemplate;
    private final JsonMapper jsonMapper;
    private final IdempotencyProperties properties;

    public IdempotencyService(
            StringRedisTemplate stringRedisTemplate, JsonMapper jsonMapper, IdempotencyProperties properties) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.jsonMapper = jsonMapper;
        this.properties = properties;
    }

    public <T> IdempotencyExecutionResult<T> execute(
            String scope, String key, String fingerprint, Class<T> targetClass, Supplier<T> action) {
        return execute(scope, key, fingerprint, targetClass, properties.lockTtl(), action);
    }

    public <T> IdempotencyExecutionResult<T> execute(
            String scope, String key, String fingerprint, Class<T> targetClass, Duration lockTtl, Supplier<T> action) {
        String redisKey = properties.keyPrefix() + scope + ":" + key;
        boolean acquired = Boolean.TRUE.equals(
                stringRedisTemplate.opsForValue().setIfAbsent(redisKey, IN_PROGRESS_PREFIX + fingerprint, lockTtl));
        if (!acquired) {
            return replayExisting(redisKey, key, fingerprint, targetClass);
        }
        try {
            T result = action.get();
            String payload = jsonMapper.writeValueAsString(result);
            String record = jsonMapper.writeValueAsString(new IdempotentResponseRecord(fingerprint, payload));
            stringRedisTemplate.opsForValue().set(redisKey, record, properties.recordTtl());
            return new IdempotencyExecutionResult<>(result, false);
        } catch (RuntimeException exception) {
            log.warn("Idempotent execution failed, evicting lock for key {} in scope {}: {}",
                    key, scope, exception.getMessage());
            stringRedisTemplate.delete(redisKey);
            throw exception;
        }
    }

    private <T> IdempotencyExecutionResult<T> replayExisting(
            String redisKey, String key, String fingerprint, Class<T> targetClass) {
        String existing = stringRedisTemplate.opsForValue().get(redisKey);
        if (existing == null) {
            log.warn("Idempotency lock vanished during acquisition for key {}", key);
            throw new ConcurrentIdempotencyException(key);
        }
        if (existing.startsWith(IN_PROGRESS_PREFIX)) {
            if (!existing.substring(IN_PROGRESS_PREFIX.length()).equals(fingerprint)) {
                throw new IdempotencyKeyMismatchException(key);
            }
            log.warn("In-flight idempotency collision detected for key {}", key);
            throw new ConcurrentIdempotencyException(key);
        }
        IdempotentResponseRecord record = jsonMapper.readValue(existing, IdempotentResponseRecord.class);
        if (!record.fingerprint().equals(fingerprint)) {
            throw new IdempotencyKeyMismatchException(key);
        }
        log.debug("Idempotency replay for key {}", key);
        return new IdempotencyExecutionResult<>(jsonMapper.readValue(record.payload(), targetClass), true);
    }
}
