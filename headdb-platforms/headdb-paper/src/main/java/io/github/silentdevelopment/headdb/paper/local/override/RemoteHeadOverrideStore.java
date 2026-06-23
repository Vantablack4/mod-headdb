package io.github.silentdevelopment.headdb.paper.local.override;

import io.github.silentdevelopment.headdb.model.HeadId;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

public interface RemoteHeadOverrideStore {

    @NotNull Optional<RemoteHeadOverride> find(@NotNull HeadId id);

    @NotNull Collection<RemoteHeadOverride> list();

    void save(@NotNull RemoteHeadOverride override);

    boolean delete(@NotNull HeadId id);

    int deleteOrphans(@NotNull Set<HeadId> validRemoteIds);
}
