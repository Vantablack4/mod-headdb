package io.github.silentdevelopment.headdb.paper.gui;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

public record MenuState(@NotNull UUID viewerId) {

    public MenuState {
        Objects.requireNonNull(viewerId, "viewerId");
    }

}