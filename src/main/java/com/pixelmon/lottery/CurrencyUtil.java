package com.pixelmon.lottery;

import com.pixelmon.lottery.PixelmonLottery.Currency;
import com.pixelmonmod.pixelmon.Pixelmon;
import net.minecraft.entity.player.ServerPlayerEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.UUID;

/**
 * Utility class for managing currencies.
 * This class provides methods to check balances, add, and remove currencies.
 *
 * NOTE: This is a placeholder implementation. You should replace the token and gem
 * implementations with your server's actual economy system.
 */
public class CurrencyUtil {
    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Check if a player has enough of a specific currency
     *
     * @param player The player to check
     * @param currency The currency type
     * @param amount The amount needed
     * @return true if player has enough, false otherwise
     */
    public static boolean hasCurrency(ServerPlayerEntity player, Currency currency, int amount) {
        if (player == null) return false;

        UUID playerId = player.getUUID();
        return switch (currency) {
            case POKECOINS -> Pixelmon.moneyManager.getBankAccount(playerId).getMoney() >= amount;

            case TOKENS -> {
                // TODO: Implement token balance check based on your server's token system
                // Example implementation:
                // return TokenManager.getTokens(playerId) >= amount;
                yield true; // Placeholder - always return true for now
            }

            case GEMS -> {
                // TODO: Implement gem balance check based on your server's gem system
                // Example implementation:
                // return GemManager.getGems(playerId) >= amount;
                yield true; // Placeholder - always return true for now
            }
        };
    }

    /**
     * Get the balance of a specific currency for a player
     *
     * @param player The player
     * @param currency The currency type
     * @return The balance amount
     */
    public static int getBalance(ServerPlayerEntity player, Currency currency) {
        if (player == null) return 0;

        UUID playerId = player.getUUID();
        return switch (currency) {
            case POKECOINS -> Pixelmon.moneyManager.getBankAccount(playerId).getMoney();

            case TOKENS -> {
                // TODO: Implement token balance fetch based on your server's token system
                // Example implementation:
                // yield TokenManager.getTokens(playerId);
                yield 1000; // Placeholder - return 1000 for now
            }

            case GEMS -> {
                // TODO: Implement gem balance fetch based on your server's gem system
                // Example implementation:
                // yield GemManager.getGems(playerId);
                yield 500; // Placeholder - return 500 for now
            }
        };
    }

    /**
     * Add currency to a player's balance
     *
     * @param player The player
     * @param currency The currency type
     * @param amount The amount to add
     */
    public static void addCurrency(ServerPlayerEntity player, Currency currency, int amount) {
        if (player == null || amount <= 0) return;

        UUID playerId = player.getUUID();
        switch (currency) {
            case POKECOINS -> Pixelmon.moneyManager.getBankAccount(playerId).add(amount);

            case TOKENS -> {
                // TODO: Implement token addition based on your server's token system
                // Example implementation:
                // TokenManager.addTokens(playerId, amount);
                LOGGER.info("Added {} {} tokens to {}", amount, currency.getName(), player.getName().getString());
            }

            case GEMS -> {
                // TODO: Implement gem addition based on your server's gem system
                // Example implementation:
                // GemManager.addGems(playerId, amount);
                LOGGER.info("Added {} {} gems to {}", amount, currency.getName(), player.getName().getString());
            }
        }
    }

    /**
     * Remove currency from a player's balance
     *
     * @param player The player
     * @param currency The currency type
     * @param amount The amount to remove
     * @return true if successful, false if player doesn't have enough
     */
    public static boolean removeCurrency(ServerPlayerEntity player, Currency currency, int amount) {
        if (player == null || amount <= 0) return false;

        // Check if player has enough first
        if (!hasCurrency(player, currency, amount)) {
            return false;
        }

        UUID playerId = player.getUUID();
        switch (currency) {
            case POKECOINS -> Pixelmon.moneyManager.getBankAccount(playerId).take(amount);

            case TOKENS -> {
                // TODO: Implement token removal based on your server's token system
                // Example implementation:
                // TokenManager.removeTokens(playerId, amount);
                LOGGER.info("Removed {} {} tokens from {}", amount, currency.getName(), player.getName().getString());
            }

            case GEMS -> {
                // TODO: Implement gem removal based on your server's gem system
                // Example implementation:
                // GemManager.removeGems(playerId, amount);
                LOGGER.info("Removed {} {} gems from {}", amount, currency.getName(), player.getName().getString());
            }
        }

        return true;
    }

    /**
     * Format currency amount with proper name
     *
     * @param currency The currency type
     * @param amount The amount
     * @param includeColor Whether to include color codes
     * @return Formatted string
     */
    public static String formatCurrency(Currency currency, int amount, boolean includeColor) {
        String colorCode = switch (currency) {
            case POKECOINS -> "§e"; // Yellow
            case TOKENS -> "§b";    // Aqua
            case GEMS -> "§a";      // Green
        };

        return includeColor ?
                colorCode + amount + " " + currency.getName() + "§r" :
                amount + " " + currency.getName();
    }
}