package io.github.silentdevelopment.headdb.paper.service;

import io.github.silentdevelopment.headdb.HeadDBService;
import io.github.silentdevelopment.headdb.category.CustomCategories;
import io.github.silentdevelopment.headdb.database.HeadDatabase;
import io.github.silentdevelopment.headdb.favorite.HeadFavorites;
import io.github.silentdevelopment.headdb.head.CustomHeads;
import io.github.silentdevelopment.headdb.paper.HeadDBPlugin;
import io.github.silentdevelopment.headdb.registry.HeadRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class PaperHeadDBService implements HeadDBService {

    private final HeadDBPlugin plugin;
    private final CustomHeads customHeads;
    private final HeadFavorites favorites;
    private final CustomCategories customCategories;

    public PaperHeadDBService(@NotNull HeadDBPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.customHeads = new PaperCustomHeads(plugin);
        this.favorites = new PaperHeadFavorites(plugin);
        this.customCategories = new PaperCustomCategories(plugin);
    }

    @Override
    public @NotNull HeadDatabase remoteDatabase() {
        return plugin.runtime().database();
    }

    @Override
    public @NotNull HeadRegistry registry() {
        return plugin.headRegistry();
    }

    @Override
    public @NotNull CustomHeads customHeads() {
        return customHeads;
    }

    @Override
    public @NotNull HeadFavorites favorites() {
        return favorites;
    }

    @Override
    public @NotNull CustomCategories customCategories() {
        return customCategories;
    }
}