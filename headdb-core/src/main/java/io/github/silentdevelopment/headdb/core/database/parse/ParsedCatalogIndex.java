package io.github.silentdevelopment.headdb.core.database.parse;

import io.github.silentdevelopment.headdb.core.remote.RemoteArtifact;
import io.github.silentdevelopment.headdb.model.HeadCategory;
import io.github.silentdevelopment.headdb.model.HeadCollection;
import io.github.silentdevelopment.headdb.model.HeadTag;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public record ParsedCatalogIndex(int schema, @NotNull String id, @NotNull List<HeadCategory> categories, @NotNull List<HeadTag> tags, @NotNull List<HeadCollection> collections, @NotNull List<RemoteArtifact> artifacts) {

    public static final int SUPPORTED_SCHEMA = 1;

    public ParsedCatalogIndex {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(categories, "categories");
        Objects.requireNonNull(tags, "tags");
        Objects.requireNonNull(collections, "collections");
        Objects.requireNonNull(artifacts, "artifacts");
        id = id.trim();
        if (schema != SUPPORTED_SCHEMA) {
            throw new IllegalArgumentException("Unsupported catalog index schema: " + schema);
        }
        if (id.isEmpty()) {
            throw new IllegalArgumentException("Catalog index ID cannot be empty.");
        }
        categories = List.copyOf(categories);
        tags = List.copyOf(tags);
        collections = List.copyOf(collections);
        artifacts = validateArtifacts(artifacts, "catalog index", "catalog-part-");
    }

    static @NotNull List<RemoteArtifact> validateArtifacts(@NotNull List<RemoteArtifact> artifacts, @NotNull String owner, @NotNull String idPrefix) {
        List<RemoteArtifact> sorted = new ArrayList<>(artifacts);
        if (sorted.isEmpty()) {
            throw new IllegalArgumentException(owner + " must contain at least one part artifact.");
        }
        sorted.sort(Comparator.comparingInt(RemoteArtifact::index));
        for (int position = 0; position < sorted.size(); position++) {
            RemoteArtifact artifact = sorted.get(position);
            if (artifact.index() != position) {
                throw new IllegalArgumentException(owner + " part indexes must be contiguous from 0. Expected " + position + " but found " + artifact.index() + ".");
            }
            String expectedId = idPrefix + String.format(Locale.ROOT, "%04d", position);
            if (!artifact.id().equals(expectedId)) {
                throw new IllegalArgumentException(owner + " part ID must be " + expectedId + " but found " + artifact.id() + ".");
            }
        }
        return List.copyOf(sorted);
    }
}
