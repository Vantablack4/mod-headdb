package io.github.silentdevelopment.headdb.model;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

public record Head(@NotNull HeadId id, @NotNull String name, @NotNull HeadTexture texture, @NotNull String category, @NotNull Set<String> tags, @NotNull Set<String> collections) {

    public Head {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(texture, "texture");
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(tags, "tags");
        Objects.requireNonNull(collections, "collections");

        name = name.trim();
        category = category.trim().toLowerCase(Locale.ROOT);

        if (name.isEmpty()) {
            throw new IllegalArgumentException("Head name cannot be empty.");
        }

        if (category.isEmpty()) {
            throw new IllegalArgumentException("Head category cannot be empty.");
        }

        tags = tags.stream().map(Head::normalizeTag).collect(Collectors.toUnmodifiableSet());
        collections = collections.stream().map(Head::normalizeCollection).collect(Collectors.toUnmodifiableSet());
    }

    private static @NotNull String normalizeTag(@NotNull String tag) {
        Objects.requireNonNull(tag, "tag");

        String normalized = tag.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Head tag cannot be empty.");
        }

        return normalized;
    }

    private static @NotNull String normalizeCollection(@NotNull String collection) {
        Objects.requireNonNull(collection, "collection");

        String normalized = collection.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Head collection cannot be empty.");
        }

        return normalized;
    }
}