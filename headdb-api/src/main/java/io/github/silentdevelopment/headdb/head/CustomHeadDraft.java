package io.github.silentdevelopment.headdb.head;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public record CustomHeadDraft(
        @NotNull String id,
        @NotNull String name,
        @NotNull String textureHash,
        @Nullable String textureSignature,
        @NotNull String category,
        @NotNull Set<String> tags,
        @NotNull Set<String> collections,
        @NotNull List<String> lore,
        @Nullable UUID createdBy
) {
}