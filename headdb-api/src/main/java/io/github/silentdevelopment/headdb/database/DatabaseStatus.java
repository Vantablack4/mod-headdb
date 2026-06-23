package io.github.silentdevelopment.headdb.database;

import java.time.Instant;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record DatabaseStatus(@NotNull DatabaseState state, @NotNull DatabaseSource source, @NotNull DatabaseStats stats, @Nullable Instant loadedAt, @Nullable String manifestId, @Nullable String artifactId, @Nullable String lastError) {

    public DatabaseStatus {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(stats, "stats");

        if (manifestId != null) {
            manifestId = manifestId.trim();
            if (manifestId.isEmpty()) {
                throw new IllegalArgumentException("Manifest ID cannot be empty.");
            }
        }

        if (artifactId != null) {
            artifactId = artifactId.trim();
            if (artifactId.isEmpty()) {
                throw new IllegalArgumentException("Artifact ID cannot be empty.");
            }
        }

        if (lastError != null) {
            lastError = lastError.trim();
            if (lastError.isEmpty()) {
                throw new IllegalArgumentException("Catalog error cannot be empty.");
            }
        }
    }

    public static @NotNull DatabaseStatus notLoaded() {
        return new DatabaseStatus(DatabaseState.NOT_LOADED, DatabaseSource.NONE, DatabaseStats.empty(), null, null, null, null);
    }

    public static @NotNull DatabaseStatus loading(@NotNull DatabaseSource source) {
        Objects.requireNonNull(source, "source");
        return new DatabaseStatus(DatabaseState.LOADING, source, DatabaseStats.empty(), null, null, null, null);
    }

    public static @NotNull DatabaseStatus loaded(@NotNull DatabaseSource source, @NotNull DatabaseStats stats, @NotNull Instant loadedAt, @NotNull String manifestId, @NotNull String artifactId) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(stats, "stats");
        Objects.requireNonNull(loadedAt, "loadedAt");
        Objects.requireNonNull(manifestId, "manifestId");
        Objects.requireNonNull(artifactId, "artifactId");

        return new DatabaseStatus(DatabaseState.LOADED, source, stats, loadedAt, manifestId, artifactId, null);
    }

    public static @NotNull DatabaseStatus failed(@NotNull String error) {
        Objects.requireNonNull(error, "error");
        return new DatabaseStatus(DatabaseState.FAILED, DatabaseSource.NONE, DatabaseStats.empty(), null, null, null, error);
    }

    public @NotNull DatabaseStatus withLastError(@NotNull String error) {
        Objects.requireNonNull(error, "error");
        return new DatabaseStatus(state, source, stats, loadedAt, manifestId, artifactId, error);
    }

    public boolean available() {
        return state == DatabaseState.LOADED;
    }
}