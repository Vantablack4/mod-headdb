package io.github.silentdevelopment.headdb.paper.gui.favorites;

import io.github.silentdevelopment.headdb.model.HeadId;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public final class FavoriteClickGuard {

    private static final long TTL_NANOS = TimeUnit.SECONDS.toNanos(1);
    private static final Map<Key, Long> CLICKS = new ConcurrentHashMap<>();

    private FavoriteClickGuard() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    public static void mark(@NotNull Player player, @NotNull HeadId id) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(id, "id");
        CLICKS.put(new Key(player.getUniqueId(), id), System.nanoTime() + TTL_NANOS);
    }

    public static boolean consume(@NotNull Player player, @NotNull HeadId id) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(id, "id");
        Long expiresAt = CLICKS.remove(new Key(player.getUniqueId(), id));
        return expiresAt != null && expiresAt >= System.nanoTime();
    }

    private record Key(@NotNull UUID playerId, @NotNull HeadId headId) {
        private Key {
            Objects.requireNonNull(playerId, "playerId");
            Objects.requireNonNull(headId, "headId");
        }
    }
}
