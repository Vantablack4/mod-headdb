package io.github.silentdevelopment.headdb.paper.gui.search;

import io.github.silentdevelopment.headdb.paper.search.SearchRequest;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

public record SearchMenuState(@NotNull UUID viewerId, @NotNull SearchRequest request) {

    public SearchMenuState {
        Objects.requireNonNull(viewerId, "viewerId");
        Objects.requireNonNull(request, "request");
    }

}