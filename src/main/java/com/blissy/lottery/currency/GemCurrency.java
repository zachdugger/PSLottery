package com.blissy.lottery.currency;

import com.blissy.gemextension.GemExtensionPlugin;
import org.bukkit.entity.Player;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Implementation of Currency interface for GemExtension
 */
public class GemCurrency implements Currency {
    private final GemExtensionPlugin gemPlugin;

    public GemCurrency(GemExtensionPlugin gemPlugin) {
        this.gemPlugin = gemPlugin;
    }

    @Override
    public String getName() {
        return "Gems";
    }

    @Override
    public String getSymbol() {
        return "♦";
    }

    @Override
    public String formatAmount(long amount) {
        return NumberFormat.getNumberInstance(Locale.US).format(amount) + " Gems";
    }

    @Override
    public long getBalance(Player player) {
        return gemPlugin.getGems(player);
    }

    @Override
    public boolean withdraw(Player player, long amount) {
        return gemPlugin.removeGems(player, amount);
    }

    @Override
    public boolean deposit(Player player, long amount) {
        return gemPlugin.addGems(player, amount);
    }

    @Override
    public boolean hasBalance(Player player, long amount) {
        return getBalance(player) >= amount;
    }
}