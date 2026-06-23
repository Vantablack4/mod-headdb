package io.github.silentdevelopment.headdb.category;

import io.github.silentdevelopment.headdb.model.HeadId;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public record CustomHeadCategory(
        @NotNull String id,
        @NotNull String name,
        @NotNull CustomCategoryIcon icon,
        @NotNull Set<HeadId> headIds
) {
}