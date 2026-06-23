package io.github.silentdevelopment.headdb.paper.local.storage;

import io.github.silentdevelopment.headdb.model.HeadId;
import io.github.silentdevelopment.headdb.paper.local.custom.CustomHeadStore;
import io.github.silentdevelopment.headdb.paper.local.custom.StoredCustomHead;
import io.github.silentdevelopment.headdb.paper.local.override.RemoteHeadOverride;
import io.github.silentdevelopment.headdb.paper.local.override.RemoteHeadOverrideStore;
import io.github.silentdevelopment.headdb.paper.local.player.CachedPlayerHead;
import io.github.silentdevelopment.headdb.paper.local.player.PlayerHeadCache;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class NoopLocalStores {

    private static final CustomHeadStore CUSTOM = new CustomHeadStore() {
        @Override public @NotNull Optional<StoredCustomHead> findStored(@NotNull HeadId id) { return Optional.empty(); }
        @Override public @NotNull Collection<StoredCustomHead> listStored() { return List.of(); }
        @Override public void save(@NotNull StoredCustomHead head) { throw new UnsupportedOperationException("Custom heads are disabled."); }
        @Override public boolean delete(@NotNull HeadId id) { return false; }
    };

    private static final RemoteHeadOverrideStore OVERRIDES = new RemoteHeadOverrideStore() {
        @Override public @NotNull Optional<RemoteHeadOverride> find(@NotNull HeadId id) { return Optional.empty(); }
        @Override public @NotNull Collection<RemoteHeadOverride> list() { return List.of(); }
        @Override public void save(@NotNull RemoteHeadOverride override) { throw new UnsupportedOperationException("Remote overrides are disabled."); }
        @Override public boolean delete(@NotNull HeadId id) { return false; }
        @Override public int deleteOrphans(@NotNull Set<HeadId> validRemoteIds) { return 0; }
    };

    private static final PlayerHeadCache PLAYER = new PlayerHeadCache() {
        @Override public @NotNull Optional<CachedPlayerHead> find(@NotNull String lookupKey) { return Optional.empty(); }
        @Override public @NotNull Collection<CachedPlayerHead> list() { return List.of(); }
        @Override public void save(@NotNull CachedPlayerHead head) {}
        @Override public boolean delete(@NotNull String lookupKey) { return false; }
    };

    private NoopLocalStores() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    public static @NotNull CustomHeadStore customHeads() { return CUSTOM; }
    public static @NotNull RemoteHeadOverrideStore remoteOverrides() { return OVERRIDES; }
    public static @NotNull PlayerHeadCache playerHeadCache() { return PLAYER; }
}
