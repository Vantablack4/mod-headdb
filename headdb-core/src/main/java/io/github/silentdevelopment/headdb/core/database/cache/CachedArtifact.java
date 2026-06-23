package io.github.silentdevelopment.headdb.core.database.cache;

import io.github.silentdevelopment.headdb.core.remote.ArtifactSelection;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public record CachedArtifact(@NotNull ArtifactSelection selection, byte @NotNull [] bytes) {

    public CachedArtifact {
        Objects.requireNonNull(selection, "selection");
        Objects.requireNonNull(bytes, "bytes");
        if (bytes.length == 0) {
            throw new IllegalArgumentException("Cached artifact bytes cannot be empty.");
        }
        bytes = bytes.clone();
    }

    @Override
    public byte @NotNull [] bytes() {
        return bytes.clone();
    }
}
