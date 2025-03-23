package com.blissy.lottery.currency;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Implementation of Currency interface for Vault/EssentialsX
 */
public class VaultCurrency implements Currency {
    private final Economy economy;

    public VaultCurrency(Economy economy) {
        this.economy = economy;
    }

    @Override
    public String getName() {
        return economy.currencyNamePlural();
    }

    @Override
    public String getSymbol() {
        return "$";
    }

    @Override
    public String formatAmount(long amount) {
        return economy.format(amount);
    }

    @Override
    public long getBalance(Player player) {
        return (long) economy.getBalance(player);
    }

    @Override
    public boolean withdraw(Player player, long amount) {
        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    @Override
    public boolean deposit(Player player, long amount) {
        return economy.depositPlayer(player, amount).transactionSuccess();
    }

    @Override
    public boolean hasBalance(Player player, long amount) {
        return economy.has(player, amount);
    }

    @Override
    public String getColoredName() {
        return "§e" + getSymbol() + " " + getName() + "§r";
    }
}