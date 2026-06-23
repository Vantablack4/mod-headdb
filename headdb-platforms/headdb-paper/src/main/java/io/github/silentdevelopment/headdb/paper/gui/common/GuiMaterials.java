package io.github.silentdevelopment.headdb.paper.gui.common;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public final class GuiMaterials {

    private GuiMaterials() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    public static @NotNull Optional<Material> item(@NotNull String value) {
        Objects.requireNonNull(value, "value");

        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (normalized.isBlank()) {
            return Optional.empty();
        }

        try {
            Material material = Material.valueOf(normalized);
            if (!modernUsableMaterial(material)) {
                return Optional.empty();
            }

            return Optional.of(material);
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    public static @NotNull Material itemOr(@NotNull String value, @NotNull Material fallback) {
        Objects.requireNonNull(fallback, "fallback");
        return item(value).orElse(fallback);
    }

    public static boolean modernUsableMaterial(@NotNull Material material) {
        Objects.requireNonNull(material, "material");

        String name = material.name();
        if (name.startsWith("LEGACY_")) {
            return false;
        }

        if (isAir(material)) {
            return false;
        }

        try {
            new ItemStack(material);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    public static boolean isAir(@NotNull Material material) {
        Objects.requireNonNull(material, "material");

        String name = material.name();
        return name.equals("AIR") || name.endsWith("_AIR");
    }
}
