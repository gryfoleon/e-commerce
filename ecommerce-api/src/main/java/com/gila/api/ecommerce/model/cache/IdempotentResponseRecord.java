package com.gila.api.ecommerce.model.cache;

public record IdempotentResponseRecord(String fingerprint, String payload) {
}
