package io.github.silentdevelopment.headdb.paper.gui.category;

import io.github.silentdevelopment.headdb.model.HeadId;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiMaterials;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public record CustomCategory(@NotNull String id, @NotNull String name, @NotNull String material, @NotNull Set<HeadId> headIds) {

    public CustomCategory(@NotNull String id, @NotNull String name, @NotNull String material) {
        this(id, name, material, Set.of());
    }

    public CustomCategory {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(material, "material");
        Objects.requireNonNull(headIds, "headIds");

        id = id.trim().toLowerCase(Locale.ROOT);
        name = name.trim();
        material = normalizeIcon(material);
        headIds = Set.copyOf(new LinkedHashSet<>(headIds));

        if (id.isBlank()) {
            throw new IllegalArgumentException("Custom category id cannot be blank.");
        }

        if (name.isBlank()) {
            throw new IllegalArgumentException("Custom category name cannot be blank.");
        }
    }

    public boolean headIcon() {
        return material.startsWith("HEAD:");
    }

    public @NotNull String headIconId() {
        if (!headIcon()) {
            return "";
        }

        return material.substring("HEAD:".length());
    }

    private static @NotNull String normalizeIcon(@NotNull String value) {
        String trimmed = value.trim();
        if (trimmed.toUpperCase(Locale.ROOT).startsWith("HEAD:")) {
            String id = trimmed.substring("HEAD:".length()).trim();
            if (id.isBlank()) {
                return Material.CHEST.name();
            }

            return "HEAD:" + new HeadId(id).toString();
        }

        return GuiMaterials.itemOr(trimmed, Material.CHEST).name();
    }
}
