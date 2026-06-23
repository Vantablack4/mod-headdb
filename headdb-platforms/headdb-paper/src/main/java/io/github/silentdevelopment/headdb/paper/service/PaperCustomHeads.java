package io.github.silentdevelopment.headdb.paper.service;

import io.github.silentdevelopment.headdb.head.CustomHeadDraft;
import io.github.silentdevelopment.headdb.head.CustomHeads;
import io.github.silentdevelopment.headdb.model.Head;
import io.github.silentdevelopment.headdb.model.HeadId;
import io.github.silentdevelopment.headdb.paper.HeadDBPlugin;
import io.github.silentdevelopment.headdb.paper.local.custom.CustomHeadStore;
import io.github.silentdevelopment.headdb.paper.local.custom.StoredCustomHead;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class PaperCustomHeads implements CustomHeads {

    private final HeadDBPlugin plugin;

    public PaperCustomHeads(@NotNull HeadDBPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    @Override
    public @NotNull List<Head> list() {
        return List.copyOf(store().list());
    }

    @Override
    public @NotNull Optional<Head> find(@NotNull HeadId id) {
        Objects.requireNonNull(id, "id");
        if (!id.isCustom()) {
            return Optional.empty();
        }

        return store().find(id);
    }

    @Override
    public @NotNull Optional<Head> find(@NotNull String input) {
        return find(customId(input));
    }

    @Override
    public @NotNull Head save(@NotNull CustomHeadDraft draft) {
        Objects.requireNonNull(draft, "draft");

        HeadId id = customId(draft.id());
        Optional<StoredCustomHead> existing = store().findStored(id);
        Instant now = Instant.now();
        Instant createdAt = existing.map(StoredCustomHead::createdAt).orElse(now);
        UUID createdBy = draft.createdBy() != null ? draft.createdBy() : existing.map(StoredCustomHead::createdBy).orElse(null);

        StoredCustomHead stored = new StoredCustomHead(
                id.key(),
                draft.name(),
                draft.textureHash(),
                draft.textureSignature(),
                draft.lore(),
                draft.tags(),
                draft.collections(),
                draft.category(),
                createdAt,
                now,
                createdBy
        );

        store().save(stored);
        plugin.headRegistry().onLocalMutation();
        plugin.clearItemCache();
        plugin.clearSearchCache();
        return stored.toHead();
    }

    @Override
    public boolean delete(@NotNull HeadId id) {
        Objects.requireNonNull(id, "id");
        if (!id.isCustom()) {
            return false;
        }

        boolean deleted = store().delete(id);
        if (deleted) {
            plugin.headRegistry().onLocalMutation();
            plugin.clearItemCache();
            plugin.clearSearchCache();
        }

        return deleted;
    }

    @Override
    public boolean delete(@NotNull String input) {
        return delete(customId(input));
    }

    private @NotNull CustomHeadStore store() {
        return plugin.headRegistry().customHeads();
    }

    private static @NotNull HeadId customId(@NotNull String input) {
        String value = input.trim();
        HeadId id = value.contains(":") ? new HeadId(value) : HeadId.custom(value);

        if (!id.isCustom()) {
            throw new IllegalArgumentException("Expected custom head id.");
        }

        return id;
    }
}