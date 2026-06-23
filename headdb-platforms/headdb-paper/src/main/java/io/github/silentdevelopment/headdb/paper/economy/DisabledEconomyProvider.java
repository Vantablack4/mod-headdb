package io.github.silentdevelopment.headdb.paper.economy;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Objects;

public final class DisabledEconomyProvider implements EconomyProvider {

    @Override
    public boolean available() {
        return false;
    }

    @Override
    public @NotNull String format(double amount) {
        return String.format(Locale.ROOT, "%.2f", amount);
    }

    @Override
    public @NotNull EconomyWithdrawal withdraw(@NotNull Player player, double amount) {
        Objects.requireNonNull(player, "player");
        return new EconomyWithdrawal(true, "");
    }
}