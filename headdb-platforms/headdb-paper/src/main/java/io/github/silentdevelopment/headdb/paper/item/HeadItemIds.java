package io.github.silentdevelopment.headdb.paper.item;

import io.github.silentdevelopment.headdb.paper.gui.common.GuiMaterials;
import io.github.silentdevelopment.headdb.model.HeadId;
import io.github.silentdevelopment.headdb.paper.HeadDBPlugin;
import io.github.silentdevelopment.headdb.paper.command.search.SearchParser;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;

public final class HeadItemIds {

    public static final String KEY = "head_id";

    private HeadItemIds() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    public static @NotNull NamespacedKey key(@NotNull HeadDBPlugin plugin) {
        Objects.requireNonNull(plugin, "plugin");
        return new NamespacedKey(plugin, KEY);
    }

    public static @NotNull Optional<HeadId> read(@NotNull HeadDBPlugin plugin, @NotNull ItemStack item) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(item, "item");

        if (GuiMaterials.isAir(item.getType())) {
            return Optional.empty();
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return Optional.empty();
        }

        String raw = meta.getPersistentDataContainer().get(key(plugin), PersistentDataType.STRING);
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }

        try {
            return Optional.of(SearchParser.headId(raw));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }
}
