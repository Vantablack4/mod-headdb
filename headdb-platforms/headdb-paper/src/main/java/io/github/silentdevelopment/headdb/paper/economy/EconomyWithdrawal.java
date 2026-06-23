package io.github.silentdevelopment.headdb.paper.economy;

import org.jetbrains.annotations.NotNull;

public record EconomyWithdrawal(boolean success, @NotNull String message) {}