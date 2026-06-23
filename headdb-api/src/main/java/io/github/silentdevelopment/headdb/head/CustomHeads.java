package io.github.silentdevelopment.headdb.head;

import io.github.silentdevelopment.headdb.model.Head;
import io.github.silentdevelopment.headdb.model.HeadId;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public interface CustomHeads {

    @NotNull List<Head> list();

    @NotNull Optional<Head> find(@NotNull HeadId id);

    @NotNull Optional<Head> find(@NotNull String input);

    @NotNull Head save(@NotNull CustomHeadDraft draft);

    boolean delete(@NotNull HeadId id);

    boolean delete(@NotNull String input);
}