package io.github.silentdevelopment.headdb.paper.local.player;

import io.github.silentdevelopment.headdb.model.Head;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface PlayerHeadService {

    @NotNull CompletableFuture<Head> resolve(@NotNull String nameOrUuid);

    @NotNull Optional<Head> resolveCached(@NotNull String nameOrUuid);

    @NotNull Collection<PlayerHeadEntry> knownPlayers();

    @NotNull List<PlayerHeadEntry> searchKnownPlayers(@NotNull String query, int limit);
}
