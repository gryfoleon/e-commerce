package com.gila.api.ecommerce.util;

import com.gila.api.ecommerce.error.exceptions.InvalidIdempotencyKeyException;
import com.gila.api.ecommerce.error.exceptions.InvalidPreconditionException;
import com.gila.api.ecommerce.error.exceptions.MissingIdempotencyKeyException;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public final class HttpHeaderUtil {

    private static final String WEAK_PREFIX = "W/";
    private static final String WILDCARD = "*";

    private HttpHeaderUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static String requireIdempotencyKey(@Nullable String rawKey) {
        return optionalIdempotencyKey(rawKey).orElseThrow(MissingIdempotencyKeyException::new);
    }

    public static Optional<String> optionalIdempotencyKey(@Nullable String rawKey) {
        if (rawKey == null || rawKey.isBlank()) {
            return Optional.empty();
        }
        String key = rawKey.strip();
        try {
            return Optional.of(UUID.fromString(key).toString());
        } catch (IllegalArgumentException exception) {
            throw new InvalidIdempotencyKeyException(key);
        }
    }

    public static String toETag(long version) {
        return "\"" + version + "\"";
    }

    public static Optional<Long> parseIfMatch(@Nullable String ifMatch) {
        if (ifMatch == null || ifMatch.isBlank() || WILDCARD.equals(ifMatch.strip())) {
            return Optional.empty();
        }
        String value = ifMatch.strip();
        if (value.startsWith(WEAK_PREFIX)) {
            value = value.substring(WEAK_PREFIX.length());
        }
        value = value.replace("\"", "");
        try {
            return Optional.of(Long.parseLong(value));
        } catch (NumberFormatException exception) {
            throw new InvalidPreconditionException(ifMatch);
        }
    }
}
