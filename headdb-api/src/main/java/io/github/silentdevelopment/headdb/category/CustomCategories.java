package io.github.silentdevelopment.headdb.category;

import io.github.silentdevelopment.headdb.model.Head;
import io.github.silentdevelopment.headdb.model.HeadId;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface CustomCategories {

    @NotNull List<CustomHeadCategory> list();

    @NotNull Optional<CustomHeadCategory> find(@NotNull String id);

    @NotNull CustomHeadCategory save(@NotNull CustomHeadCategory category);

    boolean delete(@NotNull String id);

    @NotNull Set<HeadId> memberIds(@NotNull String id);

    @NotNull List<Head> members(@NotNull String id);

    boolean addHead(@NotNull String id, @NotNull HeadId headId);

    boolean removeHead(@NotNull String id, @NotNull HeadId headId);

    boolean containsHead(@NotNull String id, @NotNull HeadId headId);
}