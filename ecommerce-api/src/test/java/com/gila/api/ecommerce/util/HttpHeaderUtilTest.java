package com.gila.api.ecommerce.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gila.api.ecommerce.error.exceptions.InvalidIdempotencyKeyException;
import com.gila.api.ecommerce.error.exceptions.InvalidPreconditionException;
import com.gila.api.ecommerce.error.exceptions.MissingIdempotencyKeyException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class HttpHeaderUtilTest {

    private static final String KEY = "3f1c1a52-5d0e-4bb8-9a7a-0f6f4f3b9c11";

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void requireIdempotencyKey_nullOrBlank_throwsMissing(String raw) {
        assertThatThrownBy(() -> HttpHeaderUtil.requireIdempotencyKey(raw))
                .isInstanceOf(MissingIdempotencyKeyException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-a-uuid", "12345", "3f1c1a52-5d0e-4bb8-9a7a"})
    void requireIdempotencyKey_notUuid_throwsInvalid(String raw) {
        assertThatThrownBy(() -> HttpHeaderUtil.requireIdempotencyKey(raw))
                .isInstanceOf(InvalidIdempotencyKeyException.class);
    }

    @Test
    void requireIdempotencyKey_uppercaseUuid_isNormalizedToLowercase() {
        assertThat(HttpHeaderUtil.requireIdempotencyKey("  " + KEY.toUpperCase() + " ")).isEqualTo(KEY);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    void optionalIdempotencyKey_absent_returnsEmpty(String raw) {
        assertThat(HttpHeaderUtil.optionalIdempotencyKey(raw)).isEmpty();
    }

    @Test
    void optionalIdempotencyKey_present_returnsNormalizedKey() {
        assertThat(HttpHeaderUtil.optionalIdempotencyKey(KEY.toUpperCase())).contains(KEY);
    }

    @Test
    void optionalIdempotencyKey_presentButInvalid_throwsInvalid() {
        assertThatThrownBy(() -> HttpHeaderUtil.optionalIdempotencyKey("oops"))
                .isInstanceOf(InvalidIdempotencyKeyException.class);
    }

    @Test
    void toETag_version_isQuoted() {
        assertThat(HttpHeaderUtil.toETag(7)).isEqualTo("\"7\"");
    }

    @ParameterizedTest
    @ValueSource(strings = {"\"3\"", "W/\"3\"", "3", " 3 ", "W/3"})
    void parseIfMatch_numericVariants_returnsVersion(String header) {
        assertThat(HttpHeaderUtil.parseIfMatch(header)).isEqualTo(Optional.of(3L));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"*", " * ", "  "})
    void parseIfMatch_absentOrWildcard_returnsEmpty(String header) {
        assertThat(HttpHeaderUtil.parseIfMatch(header)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "\"abc\"", "W/\"x\"", "1.5", "3,4"})
    void parseIfMatch_nonNumeric_throwsInvalidPrecondition(String header) {
        assertThatThrownBy(() -> HttpHeaderUtil.parseIfMatch(header))
                .isInstanceOf(InvalidPreconditionException.class);
    }
}
