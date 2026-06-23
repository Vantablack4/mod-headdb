package io.github.silentdevelopment.headdb.core.database.parse;

import io.github.silentdevelopment.headdb.core.remote.RemoteArtifact;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public record ParsedRevocationsIndex(int schema, @NotNull String id, @NotNull List<RemoteArtifact> artifacts) {

    public static final int SUPPORTED_SCHEMA = 1;

    public ParsedRevocationsIndex {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(artifacts, "artifacts");
        id = id.trim();
        if (schema != SUPPORTED_SCHEMA) {
            throw new IllegalArgumentException("Unsupported revocations index schema: " + schema);
        }
        if (id.isEmpty()) {
            throw new IllegalArgumentException("Revocations index ID cannot be empty.");
        }
        artifacts = ParsedCatalogIndex.validateArtifacts(artifacts, "revocations index", "revocations-part-");
    }
}
