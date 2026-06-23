package io.github.silentdevelopment.headdb.paper.service;

import io.github.silentdevelopment.headdb.favorite.HeadFavorites;
import io.github.silentdevelopment.headdb.model.Head;
import io.github.silentdevelopment.headdb.model.HeadId;
import io.github.silentdevelopment.headdb.paper.HeadDBPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class PaperHeadFavorites implements HeadFavorites {

    private final HeadDBPlugin plugin;

    public PaperHeadFavorites(@NotNull HeadDBPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    @Override
    public @NotNull Set<HeadId> ids(@NotNull UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        return plugin.favorites().favorites(playerId);
    }

    @Override
    public @NotNull List<Head> heads(@NotNull UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        return ids(playerId).stream().map(plugin.headRegistry()::find).flatMap(java.util.Optional::stream).toList();
    }

    @Override
    public boolean isFavorite(@NotNull UUID playerId, @NotNull HeadId headId) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(headId, "headId");
        return plugin.favorites().isFavorite(playerId, headId);
    }

    @Override
    public boolean add(@NotNull UUID playerId, @NotNull HeadId headId) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(headId, "headId");

        if (isFavorite(playerId, headId)) {
            return false;
        }

        plugin.favorites().toggle(playerId, headId);
        return true;
    }

    @Override
    public boolean remove(@NotNull UUID playerId, @NotNull HeadId headId) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(headId, "headId");

        if (!isFavorite(playerId, headId)) {
            return false;
        }

        plugin.favorites().toggle(playerId, headId);
        return true;
    }

    @Override
    public boolean toggle(@NotNull UUID playerId, @NotNull HeadId headId) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(headId, "headId");
        return plugin.favorites().toggle(playerId, headId);
    }
}