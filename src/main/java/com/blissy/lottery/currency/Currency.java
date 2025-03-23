package com.blissy.lottery.currency;

import org.bukkit.entity.Player;

/**
 * Interface for currency types used in the lottery system.
 */
public interface Currency {
    /**
     * Get the name of the currency.
     * @return The currency name
     */
    String getName();

    /**
     * Get the symbol of the currency.
     * @return The currency symbol
     */
    String getSymbol();

    /**
     * Format a currency amount into a readable string.
     * @param amount The amount to format
     * @return The formatted amount
     */
    String formatAmount(long amount);

    /**
     * Get a player's balance for this currency.
     * @param player The player
     * @return The balance
     */
    long getBalance(Player player);

    /**
     * Withdraw currency from a player.
     * @param player The player
     * @param amount The amount to withdraw
     * @return True if successful, false otherwise
     */
    boolean withdraw(Player player, long amount);

    /**
     * Deposit currency to a player.
     * @param player The player
     * @param amount The amount to deposit
     * @return True if successful, false otherwise
     */
    boolean deposit(Player player, long amount);

    /**
     * Check if a player has enough of this currency.
     * @param player The player
     * @param amount The amount to check
     * @return True if the player has enough, false otherwise
     */
    boolean hasBalance(Player player, long amount);

    /**
     * Get the identifier for this currency type.
     * @return The currency identifier
     */
    default String getId() {
        return getName().toLowerCase().replace(" ", "_");
    }

    /**
     * Get the display name with color code.
     * @return The colored display name
     */
    default String getColoredName() {
        return getSymbol() + " " + getName();
    }
}