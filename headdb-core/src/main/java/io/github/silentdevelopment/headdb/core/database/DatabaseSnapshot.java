package io.github.silentdevelopment.headdb.core.database;

import io.github.silentdevelopment.headdb.core.database.revocation.RevocationSet;
import io.github.silentdevelopment.headdb.database.DatabaseSource;
import io.github.silentdevelopment.headdb.database.DatabaseStats;
import io.github.silentdevelopment.headdb.database.DatabaseStatus;
import io.github.silentdevelopment.headdb.model.Head;
import io.github.silentdevelopment.headdb.model.HeadCategory;
import io.github.silentdevelopment.headdb.model.HeadCollection;
import io.github.silentdevelopment.headdb.model.HeadId;
import io.github.silentdevelopment.headdb.model.HeadTag;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public final class DatabaseSnapshot {

    private final String manifestId;
    private final String artifactId;
    private final DatabaseSource source;
    private final Instant loadedAt;
    private final List<Head> heads;
    private final List<HeadCategory> categories;
    private final List<HeadTag> tags;
    private final List<HeadCollection> collections;
    private final Map<HeadId, Head> headIndex;
    private final Map<String, HeadCategory> categoryIndex;
    private final Map<String, HeadTag> tagIndex;
    private final Map<String, HeadCollection> collectionIndex;
    private final RevocationSet revocations;

    public DatabaseSnapshot(@NotNull String manifestId, @NotNull String artifactId, @NotNull DatabaseSource source, @NotNull Instant loadedAt, @NotNull List<Head> heads, @NotNull List<HeadCategory> categories, @NotNull List<HeadTag> tags, @NotNull List<HeadCollection> collections, @NotNull RevocationSet revocations) {
        this.manifestId = requireText(manifestId, "manifestId");
        this.artifactId = requireText(artifactId, "artifactId");
        this.source = Objects.requireNonNull(source, "source");
        this.loadedAt = Objects.requireNonNull(loadedAt, "loadedAt");
        this.heads = List.copyOf(Objects.requireNonNull(heads, "heads"));
        this.categories = List.copyOf(Objects.requireNonNull(categories, "categories"));
        this.tags = List.copyOf(Objects.requireNonNull(tags, "tags"));
        this.collections = List.copyOf(Objects.requireNonNull(collections, "collections"));
        this.revocations = Objects.requireNonNull(revocations, "revocations");
        this.headIndex = indexHeads(this.heads);
        this.categoryIndex = indexCategories(this.categories);
        this.tagIndex = indexTags(this.tags);
        this.collectionIndex = indexCollections(this.collections);

        validateHeadReferences();
    }

    public @NotNull String manifestId() {
        return manifestId;
    }

    public @NotNull String artifactId() {
        return artifactId;
    }

    public @NotNull DatabaseSource source() {
        return source;
    }

    public @NotNull Instant loadedAt() {
        return loadedAt;
    }

    public @NotNull List<Head> heads() {
        return heads;
    }

    public @NotNull List<HeadCategory> categories() {
        return categories;
    }

    public @NotNull List<HeadTag> tags() {
        return tags;
    }

    public @NotNull List<HeadCollection> collections() {
        return collections;
    }

    public @NotNull Map<HeadId, Head> headIndex() {
        return headIndex;
    }

    public @NotNull Map<String, HeadCategory> categoryIndex() {
        return categoryIndex;
    }

    public @NotNull Map<String, HeadTag> tagIndex() {
        return tagIndex;
    }

    public @NotNull Map<String, HeadCollection> collectionIndex() {
        return collectionIndex;
    }

    public @NotNull RevocationSet revocations() {
        return revocations;
    }

    public @NotNull DatabaseStats stats() {
        return new DatabaseStats(heads.size(), categories.size(), tags.size(), collections.size(), revocations.size());
    }

    public @NotNull DatabaseStatus status() {
        return DatabaseStatus.loaded(source, stats(), loadedAt, manifestId, artifactId);
    }

    public static @NotNull DatabaseSnapshot empty() {
        return new DatabaseSnapshot("empty", "empty", DatabaseSource.NONE, Instant.EPOCH, List.of(), List.of(), List.of(), List.of(), RevocationSet.empty());
    }

    private void validateHeadReferences() {
        List<String> errors = new ArrayList<>();

        for (Head head : heads) {
            if (!categoryIndex.containsKey(head.category())) {
                errors.add("Head " + head.id() + " references unknown category: " + head.category());
            }

            for (String tag : head.tags()) {
                if (!tagIndex.containsKey(tag)) {
                    errors.add("Head " + head.id() + " references unknown tag: " + tag);
                }
            }

            for (String collection : head.collections()) {
                if (!collectionIndex.containsKey(collection)) {
                    errors.add("Head " + head.id() + " references unknown collection: " + collection);
                }
            }
        }

        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join(System.lineSeparator(), errors));
        }
    }

    private static @NotNull Map<HeadId, Head> indexHeads(@NotNull List<Head> heads) {
        Map<HeadId, Head> index = new LinkedHashMap<>();

        for (Head head : heads) {
            Head previous = index.put(head.id(), head);
            if (previous != null) {
                throw new IllegalArgumentException("Duplicate head ID: " + head.id());
            }
        }

        return Map.copyOf(index);
    }

    private static @NotNull Map<String, HeadCategory> indexCategories(@NotNull List<HeadCategory> categories) {
        Map<String, HeadCategory> index = new LinkedHashMap<>();

        for (HeadCategory category : categories) {
            HeadCategory previous = index.put(category.id(), category);
            if (previous != null) {
                throw new IllegalArgumentException("Duplicate category ID: " + category.id());
            }
        }

        return Map.copyOf(index);
    }

    private static @NotNull Map<String, HeadTag> indexTags(@NotNull List<HeadTag> tags) {
        Map<String, HeadTag> index = new LinkedHashMap<>();

        for (HeadTag tag : tags) {
            HeadTag previous = index.put(tag.id(), tag);
            if (previous != null) {
                throw new IllegalArgumentException("Duplicate tag ID: " + tag.id());
            }
        }

        return Map.copyOf(index);
    }

    private static @NotNull Map<String, HeadCollection> indexCollections(@NotNull List<HeadCollection> collections) {
        Map<String, HeadCollection> index = new LinkedHashMap<>();

        for (HeadCollection collection : collections) {
            HeadCollection previous = index.put(collection.id(), collection);
            if (previous != null) {
                throw new IllegalArgumentException("Duplicate collection ID: " + collection.id());
            }
        }

        return Map.copyOf(index);
    }

    private static @NotNull String requireText(@NotNull String value, @NotNull String name) {
        Objects.requireNonNull(value, name);

        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(name + " cannot be empty.");
        }

        return normalized;
    }
}