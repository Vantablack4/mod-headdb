package io.github.silentdevelopment.headdb.paper.economy;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class VaultEconomyProvider implements EconomyProvider {

    private final Economy economy;

    public VaultEconomyProvider(@NotNull Economy economy) {
        this.economy = Objects.requireNonNull(economy, "economy");
    }

    @Override
    public boolean available() {
        return true;
    }

    @Override
    public @NotNull String format(double amount) {
        return economy.format(amount);
    }

    @Override
    public @NotNull EconomyWithdrawal withdraw(@NotNull Player player, double amount) {
        Objects.requireNonNull(player, "player");

        EconomyResponse response = economy.withdrawPlayer(player, amount);
        if (response.transactionSuccess()) {
            return new EconomyWithdrawal(true, "");
        }

        String message = response.errorMessage == null || response.errorMessage.isBlank() ? "Transaction failed." : response.errorMessage;
        return new EconomyWithdrawal(false, message);
    }

}