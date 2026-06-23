package io.github.silentdevelopment.headdb.paper.economy;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public interface EconomyProvider {

    boolean available();

    @NotNull String format(double amount);

    @NotNull EconomyWithdrawal withdraw(@NotNull Player player, double amount);

}