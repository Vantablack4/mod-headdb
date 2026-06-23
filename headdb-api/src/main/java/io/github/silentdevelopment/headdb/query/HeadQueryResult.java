package io.github.silentdevelopment.headdb.query;

import java.util.List;
import java.util.Objects;

import io.github.silentdevelopment.headdb.model.Head;
import org.jetbrains.annotations.NotNull;

public record HeadQueryResult(@NotNull List<Head> heads, int total, int offset, int limit) {

    public HeadQueryResult {
        Objects.requireNonNull(heads, "heads");

        heads = List.copyOf(heads);

        if (total < 0) {
            throw new IllegalArgumentException("Query total cannot be negative.");
        }

        if (offset < 0) {
            throw new IllegalArgumentException("Query offset cannot be negative.");
        }

        if (limit <= 0) {
            throw new IllegalArgumentException("Query limit must be positive.");
        }

        if (heads.size() > limit) {
            throw new IllegalArgumentException("Query result cannot contain more heads than the limit.");
        }
    }

    public int pageIndex() {
        return offset / limit;
    }

    public int displayPage() {
        return pageIndex() + 1;
    }

    public int totalPages() {
        if (total == 0) {
            return 1;
        }

        return (int) Math.ceil((double) total / (double) limit);
    }

    public boolean hasPreviousPage() {
        return offset > 0;
    }

    public boolean hasNextPage() {
        return offset + heads.size() < total;
    }

    public int previousOffset() {
        return Math.max(0, offset - limit);
    }

    public int nextOffset() {
        return offset + limit;
    }

}