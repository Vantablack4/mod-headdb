package io.github.silentdevelopment.headdb.core.database.revocation;

import io.github.silentdevelopment.headdb.model.HeadId;
import io.github.silentdevelopment.headdb.model.HeadSource;
import java.time.Instant;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record HeadRevocation(@NotNull HeadId id, @Nullable String reason, @Nullable Instant revokedAt) {

    public HeadRevocation {
        Objects.requireNonNull(id, "id");

        if (id.source() != HeadSource.REMOTE) {
            throw new IllegalArgumentException("Only remote heads can be revoked.");
        }

        if (reason != null) {
            reason = reason.trim();

            if (reason.isEmpty()) {
                reason = null;
            }
        }
    }
}