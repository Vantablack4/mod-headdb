package io.github.silentdevelopment.headdb.core.hash;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public final class Sha256Verifier {

    private static final int BUFFER_SIZE = 8192;
    private static final HexFormat HEX = HexFormat.of();

    public @NotNull String hash(byte @NotNull [] bytes) {
        Objects.requireNonNull(bytes, "bytes");

        MessageDigest digest = digest();
        byte[] hash = digest.digest(bytes);

        return HEX.formatHex(hash);
    }

    public @NotNull String hash(@NotNull Path path) throws IOException {
        Objects.requireNonNull(path, "path");

        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException("SHA-256 path must be a regular file: " + path);
        }

        MessageDigest digest = digest();

        try (InputStream input = Files.newInputStream(path)) {
            byte[] buffer = new byte[BUFFER_SIZE];

            while (true) {
                int read = input.read(buffer);
                if (read == -1) {
                    break;
                }

                digest.update(buffer, 0, read);
            }
        }

        return HEX.formatHex(digest.digest());
    }

    public boolean verify(byte @NotNull [] bytes, @NotNull String expectedSha256) {
        Objects.requireNonNull(bytes, "bytes");

        String expected = normalizeSha256(expectedSha256);
        String actual = hash(bytes);

        return actual.equals(expected);
    }

    public boolean verify(@NotNull Path path, @NotNull String expectedSha256) throws IOException {
        Objects.requireNonNull(path, "path");

        String expected = normalizeSha256(expectedSha256);
        String actual = hash(path);

        return actual.equals(expected);
    }

    private static @NotNull String normalizeSha256(@NotNull String sha256) {
        Objects.requireNonNull(sha256, "sha256");

        String normalized = sha256.trim().toLowerCase();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("SHA-256 cannot be empty.");
        }

        if (normalized.length() != 64) {
            throw new IllegalArgumentException("SHA-256 must be 64 hexadecimal characters.");
        }

        for (int index = 0; index < normalized.length(); index++) {
            char character = normalized.charAt(index);

            if (character >= '0' && character <= '9') {
                continue;
            }

            if (character >= 'a' && character <= 'f') {
                continue;
            }

            throw new IllegalArgumentException("SHA-256 must be hexadecimal.");
        }

        return normalized;
    }

    private static @NotNull MessageDigest digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available.", exception);
        }
    }
}