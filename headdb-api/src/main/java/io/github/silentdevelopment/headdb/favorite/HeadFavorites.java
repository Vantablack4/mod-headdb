package io.github.silentdevelopment.headdb.favorite;

import io.github.silentdevelopment.headdb.model.Head;
import io.github.silentdevelopment.headdb.model.HeadId;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface HeadFavorites {

    @NotNull Set<HeadId> ids(@NotNull UUID playerId);

    @NotNull List<Head> heads(@NotNull UUID playerId);

    boolean isFavorite(@NotNull UUID playerId, @NotNull HeadId headId);

    boolean add(@NotNull UUID playerId, @NotNull HeadId headId);

    boolean remove(@NotNull UUID playerId, @NotNull HeadId headId);

    boolean toggle(@NotNull UUID playerId, @NotNull HeadId headId);
}