package io.github.silentdevelopment.headdb.category;

import org.jetbrains.annotations.NotNull;

public record CustomCategoryIcon(@NotNull Type type, @NotNull String value) {

    public enum Type {
        MATERIAL,
        HEAD
    }

    public static @NotNull CustomCategoryIcon material(@NotNull String material) {
        return new CustomCategoryIcon(Type.MATERIAL, material);
    }

    public static @NotNull CustomCategoryIcon head(@NotNull String headId) {
        return new CustomCategoryIcon(Type.HEAD, headId);
    }
}