package com.gila.api.ecommerce.configuration.properties;

import java.time.Duration;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.idempotency")
public record IdempotencyProperties(String keyPrefix, Duration lockTtl, Duration recordTtl) {

    public IdempotencyProperties {
        keyPrefix = Objects.requireNonNullElse(keyPrefix, "idempotency-key:");
        lockTtl = Objects.requireNonNullElse(lockTtl, Duration.ofSeconds(30));
        recordTtl = Objects.requireNonNullElse(recordTtl, Duration.ofHours(24));
    }
}
