package io.github.silentdevelopment.headdb.paper.search;

import io.github.silentdevelopment.headdb.query.HeadQuery;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class SearchQueries {

    private SearchQueries() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    public static @NotNull HeadQuery query(@NotNull SearchRequest request) {
        Objects.requireNonNull(request, "request");
        return query(request, request.page(), request.limit());
    }

    public static @NotNull HeadQuery query(@NotNull SearchRequest request, int page, int limit) {
        Objects.requireNonNull(request, "request");

        return HeadQuery.builder()
                .text(request.query())
                .ids(request.ids())
                .categories(request.categories())
                .tags(request.tags())
                .collections(request.collections())
                .page(page, limit)
                .sort(request.sort())
                .direction(request.direction())
                .build();
    }
}