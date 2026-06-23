package io.github.silentdevelopment.headdb.paper.gui.admin;

import io.github.silentdevelopment.headdb.paper.permission.Permissions;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AdminModeService {

    private final Set<UUID> enabled = ConcurrentHashMap.newKeySet();

    public boolean enabled(@NotNull Player player) {
        Objects.requireNonNull(player, "player");
        return Permissions.has(player, Permissions.GUI_ADMIN_MODE) && enabled.contains(player.getUniqueId());
    }

    public boolean toggle(@NotNull Player player) {
        Objects.requireNonNull(player, "player");

        if (!Permissions.has(player, Permissions.GUI_ADMIN_MODE)) {
            enabled.remove(player.getUniqueId());
            return false;
        }

        UUID id = player.getUniqueId();
        if (enabled.remove(id)) {
            return false;
        }

        enabled.add(id);
        return true;
    }

    public void disable(@NotNull Player player) {
        Objects.requireNonNull(player, "player");
        enabled.remove(player.getUniqueId());
    }

    public void clear() {
        enabled.clear();
    }
}
