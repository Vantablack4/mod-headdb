package io.github.silentdevelopment.headdb.paper.local.player;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public record PlayerHeadEntry(@NotNull String name, @Nullable UUID uuid) {}
