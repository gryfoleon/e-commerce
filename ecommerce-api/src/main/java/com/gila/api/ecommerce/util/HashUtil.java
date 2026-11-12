package com.gila.api.ecommerce.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class HashUtil {

    private static final String SHA_256 = "SHA-256";
    private static final int BUFFER_SIZE = 64 * 1024;

    private HashUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static String sha256Hex(String value) {
        return HexFormat.of().formatHex(newSha256().digest(value.getBytes(StandardCharsets.UTF_8)));
    }

    public static String sha256Hex(InputStream input) throws IOException {
        MessageDigest digest = newSha256();
        byte[] buffer = new byte[BUFFER_SIZE];
        int read;
        while ((read = input.read(buffer)) != -1) {
            digest.update(buffer, 0, read);
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static MessageDigest newSha256() {
        try {
            return MessageDigest.getInstance(SHA_256);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is not available", exception);
        }
    }
}
