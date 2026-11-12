package com.gila.api.ecommerce.service.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gila.api.ecommerce.configuration.properties.IdempotencyProperties;
import com.gila.api.ecommerce.error.exceptions.ConcurrentIdempotencyException;
import com.gila.api.ecommerce.error.exceptions.IdempotencyKeyMismatchException;
import com.gila.api.ecommerce.model.cache.IdempotencyExecutionResult;
import com.gila.api.ecommerce.model.cache.IdempotentResponseRecord;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import tools.jackson.databind.json.JsonMapper;

class IdempotencyServiceTest {

    private static final String REDIS_KEY = "idem:scope:k1";
    private static final Duration LOCK_TTL = Duration.ofSeconds(30);
    private static final Duration RECORD_TTL = Duration.ofHours(24);

    record Payload(String id, int value) {
    }

    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private StringRedisTemplate redis;
    private ValueOperations<String, String> values;
    private IdempotencyService service;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        redis = mock(StringRedisTemplate.class);
        values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        service = new IdempotencyService(redis, jsonMapper, new IdempotencyProperties("idem:", LOCK_TTL, RECORD_TTL));
    }

    private String record(String fingerprint, Payload payload) {
        return jsonMapper.writeValueAsString(
                new IdempotentResponseRecord(fingerprint, jsonMapper.writeValueAsString(payload)));
    }

    @Test
    void execute_newKey_runsActionOnceAndStoresRecordWithRecordTtl() {
        when(values.setIfAbsent(REDIS_KEY, "IN_PROGRESS:fp", LOCK_TTL)).thenReturn(true);
        AtomicInteger calls = new AtomicInteger();

        IdempotencyExecutionResult<Payload> result = service.execute("scope", "k1", "fp", Payload.class, () -> {
            calls.incrementAndGet();
            return new Payload("a", 1);
        });

        assertThat(result.isReplay()).isFalse();
        assertThat(result.payload()).isEqualTo(new Payload("a", 1));
        assertThat(calls).hasValue(1);
        verify(values).set(REDIS_KEY, record("fp", new Payload("a", 1)), RECORD_TTL);
    }

    @Test
    void execute_customLockTtl_isUsedForTheInFlightLock() {
        Duration custom = Duration.ofMinutes(10);
        when(values.setIfAbsent(REDIS_KEY, "IN_PROGRESS:fp", custom)).thenReturn(true);

        service.execute("scope", "k1", "fp", Payload.class, custom, () -> new Payload("a", 1));

        verify(values).setIfAbsent(REDIS_KEY, "IN_PROGRESS:fp", custom);
    }

    @Test
    void execute_completedRecordWithSameFingerprint_replaysWithoutRunningAction() {
        when(values.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);
        when(values.get(REDIS_KEY)).thenReturn(record("fp", new Payload("a", 1)));

        IdempotencyExecutionResult<Payload> result = service.execute("scope", "k1", "fp", Payload.class, () -> {
            throw new AssertionError("action must not run on replay");
        });

        assertThat(result.isReplay()).isTrue();
        assertThat(result.payload()).isEqualTo(new Payload("a", 1));
        verify(values, never()).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    void execute_completedRecordWithDifferentFingerprint_throwsKeyMismatch() {
        when(values.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);
        when(values.get(REDIS_KEY)).thenReturn(record("other", new Payload("a", 1)));

        assertThatThrownBy(() -> service.execute("scope", "k1", "fp", Payload.class, () -> new Payload("b", 2)))
                .isInstanceOf(IdempotencyKeyMismatchException.class);
    }

    @Test
    void execute_inFlightWithSameFingerprint_throwsConcurrentConflict() {
        when(values.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);
        when(values.get(REDIS_KEY)).thenReturn("IN_PROGRESS:fp");

        assertThatThrownBy(() -> service.execute("scope", "k1", "fp", Payload.class, () -> new Payload("b", 2)))
                .isInstanceOf(ConcurrentIdempotencyException.class);
    }

    @Test
    void execute_inFlightWithDifferentFingerprint_throwsKeyMismatch() {
        when(values.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);
        when(values.get(REDIS_KEY)).thenReturn("IN_PROGRESS:other");

        assertThatThrownBy(() -> service.execute("scope", "k1", "fp", Payload.class, () -> new Payload("b", 2)))
                .isInstanceOf(IdempotencyKeyMismatchException.class);
    }

    @Test
    void execute_lockVanishedBetweenAcquireAndRead_throwsConcurrentConflict() {
        when(values.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);
        when(values.get(REDIS_KEY)).thenReturn(null);

        assertThatThrownBy(() -> service.execute("scope", "k1", "fp", Payload.class, () -> new Payload("b", 2)))
                .isInstanceOf(ConcurrentIdempotencyException.class);
    }

    @Test
    void execute_nullLockResult_isTreatedAsNotAcquired() {
        when(values.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(null);
        when(values.get(REDIS_KEY)).thenReturn("IN_PROGRESS:fp");

        assertThatThrownBy(() -> service.execute("scope", "k1", "fp", Payload.class, () -> new Payload("b", 2)))
                .isInstanceOf(ConcurrentIdempotencyException.class);
    }

    @Test
    void execute_actionThrows_evictsLockSoClientCanRetryAndRethrows() {
        when(values.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        IllegalStateException failure = new IllegalStateException("boom");

        assertThatThrownBy(() -> service.execute("scope", "k1", "fp", Payload.class, () -> {
            throw failure;
        })).isSameAs(failure);

        verify(redis).delete(REDIS_KEY);
        verify(values, never()).set(anyString(), anyString(), eq(RECORD_TTL));
    }

    @Test
    void execute_sameKeyInDifferentScope_usesDifferentRedisKey() {
        when(values.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);

        service.execute("other", "k1", "fp", Payload.class, () -> new Payload("a", 1));

        verify(values).setIfAbsent("idem:other:k1", "IN_PROGRESS:fp", LOCK_TTL);
    }
}
