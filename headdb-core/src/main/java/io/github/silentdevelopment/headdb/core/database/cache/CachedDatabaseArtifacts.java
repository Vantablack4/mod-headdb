package io.github.silentdevelopment.headdb.core.database.cache;

import io.github.silentdevelopment.headdb.core.remote.RemoteManifest;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public record CachedDatabaseArtifacts(@NotNull String manifestJson, @NotNull RemoteManifest manifest, @NotNull CachedArtifact catalogIndex, @NotNull List<CachedArtifact> catalogParts, @NotNull CachedArtifact revocationsIndex, @NotNull List<CachedArtifact> revocationParts) {

    public CachedDatabaseArtifacts {
        Objects.requireNonNull(manifestJson, "manifestJson");
        Objects.requireNonNull(manifest, "manifest");
        Objects.requireNonNull(catalogIndex, "catalogIndex");
        Objects.requireNonNull(catalogParts, "catalogParts");
        Objects.requireNonNull(revocationsIndex, "revocationsIndex");
        Objects.requireNonNull(revocationParts, "revocationParts");
        manifestJson = manifestJson.trim();
        if (manifestJson.isEmpty()) {
            throw new IllegalArgumentException("Cached manifest JSON cannot be empty.");
        }
        catalogParts = List.copyOf(catalogParts);
        revocationParts = List.copyOf(revocationParts);
        if (catalogParts.isEmpty()) {
            throw new IllegalArgumentException("Cached catalog parts cannot be empty.");
        }
        if (revocationParts.isEmpty()) {
            throw new IllegalArgumentException("Cached revocation parts cannot be empty.");
        }
    }
}
