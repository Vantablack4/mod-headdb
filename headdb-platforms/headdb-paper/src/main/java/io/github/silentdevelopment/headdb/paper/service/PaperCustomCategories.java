package io.github.silentdevelopment.headdb.paper.service;

import io.github.silentdevelopment.headdb.category.CustomCategories;
import io.github.silentdevelopment.headdb.category.CustomCategoryIcon;
import io.github.silentdevelopment.headdb.category.CustomHeadCategory;
import io.github.silentdevelopment.headdb.model.Head;
import io.github.silentdevelopment.headdb.model.HeadId;
import io.github.silentdevelopment.headdb.paper.HeadDBPlugin;
import io.github.silentdevelopment.headdb.paper.gui.category.CustomCategory;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class PaperCustomCategories implements CustomCategories {

    private static final String HEAD_PREFIX = "HEAD:";

    private final HeadDBPlugin plugin;

    public PaperCustomCategories(@NotNull HeadDBPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    @Override
    public @NotNull List<CustomHeadCategory> list() {
        return plugin.customCategories().list().stream().map(PaperCustomCategories::toApi).toList();
    }

    @Override
    public @NotNull Optional<CustomHeadCategory> find(@NotNull String id) {
        Objects.requireNonNull(id, "id");
        return plugin.customCategories().find(id).map(PaperCustomCategories::toApi);
    }

    @Override
    public @NotNull CustomHeadCategory save(@NotNull CustomHeadCategory category) {
        Objects.requireNonNull(category, "category");

        CustomCategory paperCategory = new CustomCategory(category.id(), category.name(), iconValue(category.icon()), category.headIds());
        plugin.customCategories().save(paperCategory);
        plugin.headRegistry().onLocalMutation();

        return plugin.customCategories().find(category.id()).map(PaperCustomCategories::toApi).orElseGet(() -> toApi(paperCategory));
    }

    @Override
    public boolean delete(@NotNull String id) {
        Objects.requireNonNull(id, "id");
        boolean deleted = plugin.customCategories().delete(id);

        if (deleted) {
            plugin.headRegistry().onLocalMutation();
        }

        return deleted;
    }

    @Override
    public @NotNull Set<HeadId> memberIds(@NotNull String id) {
        Objects.requireNonNull(id, "id");
        return find(id).map(CustomHeadCategory::headIds).orElse(Set.of());
    }

    @Override
    public @NotNull List<Head> members(@NotNull String id) {
        Objects.requireNonNull(id, "id");
        return memberIds(id).stream().map(plugin.headRegistry()::find).flatMap(Optional::stream).toList();
    }

    @Override
    public boolean addHead(@NotNull String id, @NotNull HeadId headId) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(headId, "headId");

        if (containsHead(id, headId)) {
            return false;
        }

        plugin.customCategories().addHead(id, headId);
        plugin.headRegistry().onLocalMutation();
        return true;
    }

    @Override
    public boolean removeHead(@NotNull String id, @NotNull HeadId headId) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(headId, "headId");

        if (!containsHead(id, headId)) {
            return false;
        }

        plugin.customCategories().removeHead(id, headId);
        plugin.headRegistry().onLocalMutation();
        return true;
    }

    @Override
    public boolean containsHead(@NotNull String id, @NotNull HeadId headId) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(headId, "headId");
        return memberIds(id).contains(headId);
    }

    private static @NotNull CustomHeadCategory toApi(@NotNull CustomCategory category) {
        return new CustomHeadCategory(category.id(), category.name(), icon(category), category.headIds());
    }

    private static @NotNull CustomCategoryIcon icon(@NotNull CustomCategory category) {
        if (category.headIcon()) {
            return CustomCategoryIcon.head(category.headIconId());
        }

        return CustomCategoryIcon.material(category.material());
    }

    private static @NotNull String iconValue(@NotNull CustomCategoryIcon icon) {
        if (icon.type() == CustomCategoryIcon.Type.HEAD) {
            return HEAD_PREFIX + new HeadId(icon.value());
        }

        return icon.value();
    }
}