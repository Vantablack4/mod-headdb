package io.github.silentdevelopment.headdb.core.remote;

import java.net.URI;
import java.util.Locale;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public record RemoteArtifact(int index, @NotNull String id, @NotNull String path, @NotNull String contentType, @NotNull String compression, @NotNull RemoteIntegrity integrity) {

    public static final int NO_INDEX = -1;
    public static final String COMPRESSION_ZSTD = "zstd";
    public static final String COMPRESSION_NONE = "none";

    public RemoteArtifact(@NotNull String id, @NotNull String path, @NotNull String contentType, @NotNull String compression, @NotNull RemoteIntegrity integrity) {
        this(NO_INDEX, id, path, contentType, compression, integrity);
    }

    public RemoteArtifact {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(contentType, "contentType");
        Objects.requireNonNull(compression, "compression");
        Objects.requireNonNull(integrity, "integrity");
        id = id.trim();
        path = path.trim();
        contentType = contentType.trim();
        compression = compression.trim().toLowerCase(Locale.ROOT);
        if (index < NO_INDEX) {
            throw new IllegalArgumentException("Remote artifact index cannot be less than -1.");
        }
        if (id.isEmpty()) {
            throw new IllegalArgumentException("Remote artifact ID cannot be empty.");
        }
        validatePath(path);
        if (contentType.isEmpty()) {
            throw new IllegalArgumentException("Remote artifact content type cannot be empty.");
        }
        if (!compression.equals(COMPRESSION_ZSTD) && !compression.equals(COMPRESSION_NONE)) {
            throw new IllegalArgumentException("Unsupported remote artifact compression: " + compression);
        }
    }

    public @NotNull String sha256() {
        return integrity.digest();
    }

    public long size() {
        return integrity.bytes();
    }

    public static void validatePath(@NotNull String path) {
        Objects.requireNonNull(path, "path");
        String normalized = path.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Remote artifact path cannot be empty.");
        }
        if (normalized.startsWith("/")) {
            throw new IllegalArgumentException("Remote artifact path must be relative: " + path);
        }
        if (normalized.contains("\\")) {
            throw new IllegalArgumentException("Remote artifact path must use forward slashes: " + path);
        }
        URI uri = URI.create(normalized);
        if (uri.isAbsolute()) {
            throw new IllegalArgumentException("Remote artifact path must not be absolute: " + path);
        }
        for (String segment : normalized.replace('\\', '/').split("/")) {
            if (segment.equals("..")) {
                throw new IllegalArgumentException("Remote artifact path cannot contain traversal: " + path);
            }
        }
    }
}
