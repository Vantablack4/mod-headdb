package io.github.silentdevelopment.headdb.query;

import io.github.silentdevelopment.headdb.model.HeadId;
import io.github.silentdevelopment.headdb.model.HeadSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public record HeadQuery(
        @NotNull String text,
        @Nullable HeadSource source,
        @NotNull Set<HeadId> ids,
        @NotNull Set<String> categories,
        @NotNull Set<String> tags,
        @NotNull Set<String> collections,
        @NotNull HeadSort sort,
        @NotNull SortDirection direction,
        int offset,
        int limit
) {

    public static final int DEFAULT_LIMIT = 50;
    public static final int MAX_LIMIT = 500;

    public HeadQuery(
            @NotNull String text,
            @Nullable HeadSource source,
            @NotNull Set<HeadId> ids,
            @Nullable String category,
            @NotNull Set<String> tags,
            @NotNull Set<String> collections,
            @NotNull HeadSort sort,
            @NotNull SortDirection direction,
            int offset,
            int limit
    ) {
        this(text, source, ids, categorySet(category), tags, collections, sort, direction, offset, limit);
    }

    public HeadQuery {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(ids, "ids");
        Objects.requireNonNull(categories, "categories");
        Objects.requireNonNull(tags, "tags");
        Objects.requireNonNull(collections, "collections");
        Objects.requireNonNull(sort, "sort");
        Objects.requireNonNull(direction, "direction");

        text = text.trim();
        ids = Set.copyOf(ids);

        categories = categories.stream()
                .map(value -> normalizeId(value, "category"))
                .collect(Collectors.toUnmodifiableSet());

        tags = tags.stream()
                .map(value -> normalizeId(value, "tag"))
                .collect(Collectors.toUnmodifiableSet());

        collections = collections.stream()
                .map(value -> normalizeId(value, "collection"))
                .collect(Collectors.toUnmodifiableSet());

        if (offset < 0) {
            throw new IllegalArgumentException("Query offset cannot be negative.");
        }

        if (limit <= 0) {
            throw new IllegalArgumentException("Query limit must be positive.");
        }

        if (limit > MAX_LIMIT) {
            throw new IllegalArgumentException("Query limit cannot be greater than " + MAX_LIMIT + ".");
        }
    }

    public static @NotNull HeadQuery all() {
        return builder().build();
    }

    public static @NotNull HeadQuery text(@NotNull String text) {
        return builder()
                .text(text)
                .sort(HeadSort.RELEVANCE)
                .direction(SortDirection.DESCENDING)
                .build();
    }

    public static @NotNull Builder builder() {
        return new Builder();
    }

    public @Nullable String category() {
        return categories.stream().findFirst().orElse(null);
    }

    private static @NotNull Set<String> categorySet(@Nullable String category) {
        if (category == null) {
            return Set.of();
        }

        return Set.of(category);
    }

    private static @NotNull String normalizeId(@NotNull String value, @NotNull String name) {
        Objects.requireNonNull(value, name);

        String normalized = value.trim().toLowerCase(Locale.ROOT);

        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Query " + name + " cannot be empty.");
        }

        return normalized;
    }

    public static final class Builder {

        private String text;
        private HeadSource source;
        private final Set<HeadId> ids;
        private final Set<String> categories;
        private final Set<String> tags;
        private final Set<String> collections;
        private HeadSort sort;
        private SortDirection direction;
        private int offset;
        private int limit;

        private Builder() {
            this.text = "";
            this.ids = new LinkedHashSet<>();
            this.categories = new LinkedHashSet<>();
            this.tags = new LinkedHashSet<>();
            this.collections = new LinkedHashSet<>();
            this.sort = HeadSort.ID;
            this.direction = SortDirection.ASCENDING;
            this.offset = 0;
            this.limit = DEFAULT_LIMIT;
        }

        public @NotNull Builder text(@NotNull String text) {
            this.text = Objects.requireNonNull(text, "text");
            return this;
        }

        public @NotNull Builder source(@Nullable HeadSource source) {
            this.source = source;
            return this;
        }

        public @NotNull Builder id(@NotNull HeadId id) {
            this.ids.add(Objects.requireNonNull(id, "id"));
            return this;
        }

        public @NotNull Builder ids(@NotNull Iterable<HeadId> ids) {
            Objects.requireNonNull(ids, "ids");

            for (HeadId id : ids) {
                id(id);
            }

            return this;
        }

        public @NotNull Builder category(@Nullable String category) {
            this.categories.clear();

            if (category != null) {
                this.categories.add(category);
            }

            return this;
        }

        public @NotNull Builder categories(@NotNull Iterable<String> categories) {
            Objects.requireNonNull(categories, "categories");

            for (String category : categories) {
                this.categories.add(Objects.requireNonNull(category, "category"));
            }

            return this;
        }

        public @NotNull Builder tag(@NotNull String tag) {
            this.tags.add(Objects.requireNonNull(tag, "tag"));
            return this;
        }

        public @NotNull Builder tags(@NotNull Iterable<String> tags) {
            Objects.requireNonNull(tags, "tags");

            for (String tag : tags) {
                tag(tag);
            }

            return this;
        }

        public @NotNull Builder collection(@NotNull String collection) {
            this.collections.add(Objects.requireNonNull(collection, "collection"));
            return this;
        }

        public @NotNull Builder collections(@NotNull Iterable<String> collections) {
            Objects.requireNonNull(collections, "collections");

            for (String collection : collections) {
                collection(collection);
            }

            return this;
        }

        public @NotNull Builder sort(@NotNull HeadSort sort) {
            this.sort = Objects.requireNonNull(sort, "sort");
            return this;
        }

        public @NotNull Builder direction(@NotNull SortDirection direction) {
            this.direction = Objects.requireNonNull(direction, "direction");
            return this;
        }

        public @NotNull Builder offset(int offset) {
            this.offset = offset;
            return this;
        }

        public @NotNull Builder limit(int limit) {
            this.limit = limit;
            return this;
        }

        public @NotNull Builder page(int page) {
            if (page < 1) {
                throw new IllegalArgumentException("Query page must be at least 1.");
            }

            this.offset = (page - 1) * limit;
            return this;
        }

        public @NotNull Builder page(int page, int limit) {
            limit(limit);
            page(page);
            return this;
        }

        public @NotNull HeadQuery build() {
            return new HeadQuery(
                    text,
                    source,
                    Set.copyOf(ids),
                    Set.copyOf(categories),
                    Set.copyOf(tags),
                    Set.copyOf(collections),
                    sort,
                    direction,
                    offset,
                    limit
            );
        }
    }
}