package io.github.silentdevelopment.headdb.core.remote;

import java.util.Locale;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public record RemoteIntegrity(@NotNull String algorithm, @NotNull String digest, long bytes) {

    public static final String SHA256 = "sha256";

    public RemoteIntegrity {
        Objects.requireNonNull(algorithm, "algorithm");
        Objects.requireNonNull(digest, "digest");
        algorithm = algorithm.trim().toLowerCase(Locale.ROOT);
        digest = digest.trim().toLowerCase(Locale.ROOT);
        if (algorithm.isEmpty()) {
            throw new IllegalArgumentException("Remote integrity algorithm cannot be empty.");
        }
        if (!algorithm.equals(SHA256)) {
            throw new IllegalArgumentException("Unsupported remote integrity algorithm: " + algorithm);
        }
        if (digest.isEmpty()) {
            throw new IllegalArgumentException("Remote integrity digest cannot be empty.");
        }
        if (digest.length() != 64) {
            throw new IllegalArgumentException("Remote integrity SHA-256 digest must be 64 hexadecimal characters.");
        }
        if (!isHex(digest)) {
            throw new IllegalArgumentException("Remote integrity SHA-256 digest must be hexadecimal.");
        }
        if (bytes < 0) {
            throw new IllegalArgumentException("Remote integrity byte size cannot be negative.");
        }
    }

    private static boolean isHex(@NotNull String value) {
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character >= '0' && character <= '9') {
                continue;
            }
            if (character >= 'a' && character <= 'f') {
                continue;
            }
            return false;
        }
        return true;
    }
}
