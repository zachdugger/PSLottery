package com.blissy.lottery.currency;

import me.realized.tokenmanager.TokenManagerPlugin;
import org.bukkit.entity.Player;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Implementation of Currency interface for TokenManager
 */
public class TokenCurrency implements Currency {
    private final TokenManagerPlugin tokenPlugin;

    public TokenCurrency(TokenManagerPlugin tokenPlugin) {
        this.tokenPlugin = tokenPlugin;
    }

    @Override
    public String getName() {
        return "Tokens";
    }

    @Override
    public String getSymbol() {
        return "⛃";
    }

    @Override
    public String formatAmount(long amount) {
        return NumberFormat.getNumberInstance(Locale.US).format(amount) + " Tokens";
    }

    @Override
    public long getBalance(Player player) {
        return tokenPlugin.getTokenManager().getTokens(player).orElse(0L);
    }

    @Override
    public boolean withdraw(Player player, long amount) {
        return tokenPlugin.getTokenManager().removeTokens(player, amount);
    }

    @Override
    public boolean deposit(Player player, long amount) {
        return tokenPlugin.getTokenManager().addTokens(player, amount);
    }

    @Override
    public boolean hasBalance(Player player, long amount) {
        return getBalance(player) >= amount;
    }

    @Override
    public String getColoredName() {
        return "§b" + getSymbol() + " " + getName() + "§r";
    }
}