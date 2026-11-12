package com.gila.api.ecommerce.model.cache;

public record IdempotencyExecutionResult<T>(T payload, boolean isReplay) {
}
