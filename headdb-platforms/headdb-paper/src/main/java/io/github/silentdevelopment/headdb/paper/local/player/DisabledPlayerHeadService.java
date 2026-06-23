package io.github.silentdevelopment.headdb.paper.local.player;

import io.github.silentdevelopment.headdb.model.Head;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class DisabledPlayerHeadService implements PlayerHeadService {

    @Override
    public @NotNull CompletableFuture<Head> resolve(@NotNull String nameOrUuid) {
        return CompletableFuture.failedFuture(new UnsupportedOperationException("Player heads are disabled."));
    }

    @Override
    public @NotNull Optional<Head> resolveCached(@NotNull String nameOrUuid) {
        return Optional.empty();
    }

    @Override
    public @NotNull Collection<PlayerHeadEntry> knownPlayers() {
        return List.of();
    }

    @Override
    public @NotNull List<PlayerHeadEntry> searchKnownPlayers(@NotNull String query, int limit) {
        return List.of();
    }
}
