package com.gila.api.ecommerce.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Random;
import org.junit.jupiter.api.Test;

class HashUtilTest {

    private static final String EMPTY_SHA256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

    @Test
    void sha256Hex_knownValue_matchesReferenceDigest() {
        assertThat(HashUtil.sha256Hex("abc"))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }

    @Test
    void sha256Hex_stream_matchesStringDigestForSameUtf8Content() throws IOException {
        String content = "name,sku\nRatón inalámbrico,ELEC1\n";

        String fromStream = HashUtil.sha256Hex(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));

        assertThat(fromStream).isEqualTo(HashUtil.sha256Hex(content));
    }

    @Test
    void sha256Hex_emptyStream_returnsEmptyContentDigest() throws IOException {
        assertThat(HashUtil.sha256Hex(new ByteArrayInputStream(new byte[0]))).isEqualTo(EMPTY_SHA256);
        assertThat(HashUtil.sha256Hex("")).isEqualTo(EMPTY_SHA256);
    }

    @Test
    void sha256Hex_streamLargerThanBuffer_hashesEveryChunk() throws IOException {
        byte[] data = new byte[64 * 1024 * 3 + 123];
        new Random(42).nextBytes(data);
        String expected = HexFormat.of().formatHex(sha256(data));

        assertThat(HashUtil.sha256Hex(new ByteArrayInputStream(data))).isEqualTo(expected);
    }

    @Test
    void sha256Hex_differentContentSameLength_producesDifferentDigest() throws IOException {
        String first = HashUtil.sha256Hex(new ByteArrayInputStream("aaaa".getBytes(StandardCharsets.UTF_8)));
        String second = HashUtil.sha256Hex(new ByteArrayInputStream("aaab".getBytes(StandardCharsets.UTF_8)));

        assertThat(first).isNotEqualTo(second);
    }

    private static byte[] sha256(byte[] data) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(data);
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
