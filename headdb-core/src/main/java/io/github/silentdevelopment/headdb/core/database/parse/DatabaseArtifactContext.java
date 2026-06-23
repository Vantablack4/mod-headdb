package io.github.silentdevelopment.headdb.core.database.parse;

import io.github.silentdevelopment.headdb.core.database.revocation.RevocationSet;
import io.github.silentdevelopment.headdb.database.DatabaseSource;
import java.time.Instant;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public record DatabaseArtifactContext(@NotNull String manifestId, @NotNull String artifactId, @NotNull DatabaseSource source, @NotNull Instant loadedAt, @NotNull RevocationSet revocations) {

    public DatabaseArtifactContext {
        Objects.requireNonNull(manifestId, "manifestId");
        Objects.requireNonNull(artifactId, "artifactId");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(loadedAt, "loadedAt");
        Objects.requireNonNull(revocations, "revocations");

        manifestId = manifestId.trim();
        artifactId = artifactId.trim();

        if (manifestId.isEmpty()) {
            throw new IllegalArgumentException("Manifest ID cannot be empty.");
        }

        if (artifactId.isEmpty()) {
            throw new IllegalArgumentException("Artifact ID cannot be empty.");
        }
    }
}