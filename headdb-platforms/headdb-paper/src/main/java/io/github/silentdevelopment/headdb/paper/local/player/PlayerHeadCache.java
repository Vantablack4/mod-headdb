package io.github.silentdevelopment.headdb.paper.local.player;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Optional;

public interface PlayerHeadCache {

    @NotNull Optional<CachedPlayerHead> find(@NotNull String lookupKey);

    @NotNull Collection<CachedPlayerHead> list();

    void save(@NotNull CachedPlayerHead head);

    boolean delete(@NotNull String lookupKey);
}
