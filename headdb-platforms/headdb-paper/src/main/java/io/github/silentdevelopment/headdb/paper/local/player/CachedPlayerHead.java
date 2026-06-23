package io.github.silentdevelopment.headdb.paper.local.player;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record CachedPlayerHead(
        @NotNull String lookupKey,
        @Nullable UUID uuid,
        @NotNull String name,
        @Nullable String textureHash,
        @Nullable String textureSignature,
        @NotNull Instant resolvedAt,
        @Nullable Instant failedAt
) {

    public CachedPlayerHead {
        Objects.requireNonNull(lookupKey, "lookupKey");
        if (name == null || name.isBlank()) {
            name = lookupKey;
        }
        if (resolvedAt == null) {
            resolvedAt = Instant.EPOCH;
        }
        lookupKey = lookupKey.trim().toLowerCase(java.util.Locale.ROOT);
        name = name.trim();
        textureHash = textureHash == null || textureHash.isBlank() ? null : textureHash.trim().toLowerCase(java.util.Locale.ROOT);
        textureSignature = textureSignature == null || textureSignature.isBlank() ? null : textureSignature.trim();
    }
}
