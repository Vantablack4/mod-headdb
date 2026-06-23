package io.github.silentdevelopment.headdb.paper.gui.edit;

import io.github.silentdevelopment.headdb.model.HeadId;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public final class HeadEditDropGuard {

    private static final long TTL_NANOS = TimeUnit.SECONDS.toNanos(1);
    private static final Map<Key, Long> DROPS = new ConcurrentHashMap<>();

    private HeadEditDropGuard() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    public static void mark(@NotNull Player player, @NotNull HeadId id) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(id, "id");

        DROPS.put(new Key(player.getUniqueId(), id), System.nanoTime() + TTL_NANOS);
    }

    public static boolean consume(@NotNull Player player, @NotNull HeadId id) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(id, "id");

        long now = System.nanoTime();
        Key key = new Key(player.getUniqueId(), id);
        Long expiresAt = DROPS.remove(key);

        if (expiresAt == null) {
            return false;
        }

        return expiresAt >= now;
    }

    private record Key(@NotNull UUID playerId, @NotNull HeadId headId) {

        private Key {
            Objects.requireNonNull(playerId, "playerId");
            Objects.requireNonNull(headId, "headId");
        }
    }
}