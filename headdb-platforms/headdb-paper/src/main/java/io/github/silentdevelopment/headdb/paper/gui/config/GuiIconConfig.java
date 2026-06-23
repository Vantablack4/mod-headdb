package io.github.silentdevelopment.headdb.paper.gui.config;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public record GuiIconConfig(
        @NotNull String key,
        @NotNull String type,
        @NotNull String headId,
        @NotNull String material,
        @NotNull String name,
        @NotNull List<String> lore
) {

    public GuiIconConfig {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(headId, "headId");
        Objects.requireNonNull(material, "material");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(lore, "lore");

        lore = List.copyOf(lore);
    }

    public @NotNull GuiIconType iconType() {
        return GuiIconType.parse(type);
    }
}