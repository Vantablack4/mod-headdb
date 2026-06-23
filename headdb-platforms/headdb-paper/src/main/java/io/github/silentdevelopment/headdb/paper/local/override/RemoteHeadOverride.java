package io.github.silentdevelopment.headdb.paper.local.override;

import io.github.silentdevelopment.headdb.model.HeadId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record RemoteHeadOverride(
        @NotNull HeadId headId,
        @Nullable String name,
        @Nullable List<String> lore,
        @NotNull Set<String> addTags,
        @NotNull Set<String> removeTags,
        @Nullable Set<String> replaceTags,
        @NotNull Set<String> addCollections,
        @NotNull Set<String> removeCollections,
        @Nullable String category,
        @Nullable Boolean hidden,
        @NotNull Instant createdAt,
        @NotNull Instant updatedAt,
        @Nullable UUID updatedBy
) {

    public RemoteHeadOverride {
        Objects.requireNonNull(headId, "headId");
        if (addTags == null) {
            addTags = Set.of();
        }
        if (removeTags == null) {
            removeTags = Set.of();
        }
        if (addCollections == null) {
            addCollections = Set.of();
        }
        if (removeCollections == null) {
            removeCollections = Set.of();
        }
        if (createdAt == null) {
            createdAt = Instant.EPOCH;
        }
        if (updatedAt == null) {
            updatedAt = createdAt;
        }

        if (!headId.isRemote()) {
            throw new IllegalArgumentException("Remote head override ID must use the remote namespace.");
        }

        if (name != null) {
            name = name.trim();
            if (name.isEmpty()) {
                name = null;
            }
        }

        if (category != null) {
            category = normalizeId(category);
            if (category.isBlank()) {
                category = null;
            }
        }

        lore = lore == null ? null : List.copyOf(lore);
        addTags = normalizeSet(addTags);
        removeTags = normalizeSet(removeTags);
        replaceTags = replaceTags == null ? null : normalizeSet(replaceTags);
        addCollections = normalizeSet(addCollections);
        removeCollections = normalizeSet(removeCollections);
    }

    public static @NotNull RemoteHeadOverride empty(@NotNull HeadId id, @Nullable UUID updatedBy) {
        Instant now = Instant.now();
        return new RemoteHeadOverride(id, null, null, Set.of(), Set.of(), null, Set.of(), Set.of(), null, null, now, now, updatedBy);
    }

    public @NotNull RemoteHeadOverride withName(@Nullable String name, @Nullable UUID updatedBy) {
        return new RemoteHeadOverride(headId, name, lore, addTags, removeTags, replaceTags, addCollections, removeCollections, category, hidden, createdAt, Instant.now(), updatedBy);
    }

    public @NotNull RemoteHeadOverride withLore(@Nullable List<String> lore, @Nullable UUID updatedBy) {
        return new RemoteHeadOverride(headId, name, lore, addTags, removeTags, replaceTags, addCollections, removeCollections, category, hidden, createdAt, Instant.now(), updatedBy);
    }

    public @NotNull RemoteHeadOverride withCategory(@Nullable String category, @Nullable UUID updatedBy) {
        return new RemoteHeadOverride(headId, name, lore, addTags, removeTags, replaceTags, addCollections, removeCollections, category, hidden, createdAt, Instant.now(), updatedBy);
    }

    public @NotNull RemoteHeadOverride withHidden(@Nullable Boolean hidden, @Nullable UUID updatedBy) {
        return new RemoteHeadOverride(headId, name, lore, addTags, removeTags, replaceTags, addCollections, removeCollections, category, Boolean.TRUE.equals(hidden) ? Boolean.TRUE : null, createdAt, Instant.now(), updatedBy);
    }

    public @NotNull RemoteHeadOverride withTagAdded(@NotNull String tag, @Nullable UUID updatedBy) {
        Set<String> updatedAdd = new java.util.LinkedHashSet<>(addTags);
        Set<String> updatedRemove = new java.util.LinkedHashSet<>(removeTags);
        updatedAdd.add(normalizeId(tag));
        updatedRemove.remove(normalizeId(tag));
        return new RemoteHeadOverride(headId, name, lore, updatedAdd, updatedRemove, replaceTags, addCollections, removeCollections, category, hidden, createdAt, Instant.now(), updatedBy);
    }

    public @NotNull RemoteHeadOverride withTagRemoved(@NotNull String tag, @Nullable UUID updatedBy) {
        Set<String> updatedAdd = new java.util.LinkedHashSet<>(addTags);
        Set<String> updatedRemove = new java.util.LinkedHashSet<>(removeTags);
        updatedAdd.remove(normalizeId(tag));
        updatedRemove.add(normalizeId(tag));
        return new RemoteHeadOverride(headId, name, lore, updatedAdd, updatedRemove, replaceTags, addCollections, removeCollections, category, hidden, createdAt, Instant.now(), updatedBy);
    }

    public @NotNull RemoteHeadOverride withReplacementTags(@Nullable Set<String> tags, @Nullable UUID updatedBy) {
        return new RemoteHeadOverride(headId, name, lore, Set.of(), Set.of(), tags, addCollections, removeCollections, category, hidden, createdAt, Instant.now(), updatedBy);
    }

    public @NotNull RemoteHeadOverride withCollectionAdded(@NotNull String collection, @Nullable UUID updatedBy) {
        Set<String> updatedAdd = new java.util.LinkedHashSet<>(addCollections);
        Set<String> updatedRemove = new java.util.LinkedHashSet<>(removeCollections);
        updatedAdd.add(normalizeId(collection));
        updatedRemove.remove(normalizeId(collection));
        return new RemoteHeadOverride(headId, name, lore, addTags, removeTags, replaceTags, updatedAdd, updatedRemove, category, hidden, createdAt, Instant.now(), updatedBy);
    }

    public @NotNull RemoteHeadOverride withCollectionRemoved(@NotNull String collection, @Nullable UUID updatedBy) {
        Set<String> updatedAdd = new java.util.LinkedHashSet<>(addCollections);
        Set<String> updatedRemove = new java.util.LinkedHashSet<>(removeCollections);
        updatedAdd.remove(normalizeId(collection));
        updatedRemove.add(normalizeId(collection));
        return new RemoteHeadOverride(headId, name, lore, addTags, removeTags, replaceTags, updatedAdd, updatedRemove, category, hidden, createdAt, Instant.now(), updatedBy);
    }

    public @NotNull RemoteHeadOverride withReplacementCollections(@Nullable Set<String> collections, @Nullable UUID updatedBy) {
        return new RemoteHeadOverride(headId, name, lore, addTags, removeTags, replaceTags, collections == null ? Set.of() : collections, Set.of(), category, hidden, createdAt, Instant.now(), updatedBy);
    }

    public boolean empty() {
        return name == null
                && lore == null
                && addTags.isEmpty()
                && removeTags.isEmpty()
                && replaceTags == null
                && addCollections.isEmpty()
                && removeCollections.isEmpty()
                && category == null
                && hidden == null;
    }

    private static @NotNull Set<String> normalizeSet(@NotNull Set<String> values) {
        Set<String> result = new java.util.LinkedHashSet<>();
        for (String value : values) {
            String normalized = normalizeId(value);
            if (!normalized.isBlank()) {
                result.add(normalized);
            }
        }
        return Set.copyOf(result);
    }

    private static @NotNull String normalizeId(@NotNull String value) {
        Objects.requireNonNull(value, "value");
        return value.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
