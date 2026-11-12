package com.gila.api.ecommerce.configuration.properties;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.cors")
public record CorsProperties(List<String> allowedOrigins, Duration maxAge) {

    public CorsProperties {
        allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
        maxAge = Objects.requireNonNullElse(maxAge, Duration.ofHours(1));
    }
}
