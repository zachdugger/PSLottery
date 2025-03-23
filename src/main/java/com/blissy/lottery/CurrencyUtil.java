package com.blissy.lottery;

import com.blissy.lottery.currency.Currency;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class for managing currencies across different economy plugins.
 */
public class CurrencyUtil {
    private static final Logger LOGGER = Bukkit.getLogger();
    private static net.milkbowl.vault.economy.Economy vaultEconomy = null;
    private static Object tokenManager = null;
    private static Object gemExtension = null;

    /**
     * Initialize the economy hooks. This should be called during server startup.
     */
    public static void initialize() {
        try {
            // Initialize Vault Economy (for EssentialsX)
            if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
                org.bukkit.plugin.RegisteredServiceProvider<net.milkbowl.vault.economy.Economy> rsp =
                        Bukkit.getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
                if (rsp != null) {
                    vaultEconomy = rsp.getProvider();
                    LOGGER.info("Successfully hooked into Vault/EssentialsX economy");
                }
            }

            // Initialize TokenManager
            if (Bukkit.getPluginManager().getPlugin("TokenManager") != null) {
                tokenManager = Bukkit.getPluginManager().getPlugin("TokenManager");
                LOGGER.info("Successfully hooked into TokenManager");
            }

            // Initialize GemExtension
            if (Bukkit.getPluginManager().getPlugin("GemExtension") != null) {
                gemExtension = Bukkit.getPluginManager().getPlugin("GemExtension");
                LOGGER.info("Successfully hooked into GemExtension");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize economy hooks", e);
        }
    }

    /**
     * Check if a player has enough of a specific currency
     *
     * @param player The player to check
     * @param currency The currency type
     * @param amount The amount needed
     * @return true if player has enough, false otherwise
     */
    public static boolean hasCurrency(Player player, Currency currency, long amount) {
        if (player == null) return false;

        String currencyId = currency.getId();

        if (currencyId.equals("coins") || currencyId.equals("dollars")) {
            if (vaultEconomy != null) {
                return vaultEconomy.has(player, amount);
            }
            return false;
        } else if (currencyId.equals("tokens")) {
            if (tokenManager != null) {
                try {
                    // Using reflection to access TokenManager methods
                    Class<?> managerClass = Class.forName("me.realized.tokenmanager.TokenManagerPlugin");
                    Object tokenManagerInstance = managerClass.cast(tokenManager);

                    // Get the API instance
                    Object api = managerClass.getMethod("getTokenManager").invoke(tokenManagerInstance);
                    Class<?> apiClass = api.getClass();

                    // Get player's tokens
                    long tokens = (long) apiClass.getMethod("getTokens", Player.class).invoke(api, player);
                    return tokens >= amount;
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Error checking TokenManager balance", e);
                }
            }
            return false;
        } else if (currencyId.equals("gems")) {
            if (gemExtension != null) {
                try {
                    // Using reflection to access GemExtension methods
                    Class<?> gemClass = Class.forName("com.blissy.gemextension.GemExtensionPlugin");
                    Object gemInstance = gemClass.cast(gemExtension);

                    // Get player's gems
                    long gems = (long) gemClass.getMethod("getGems", Player.class).invoke(gemInstance, player);
                    return gems >= amount;
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Error checking GemExtension balance", e);
                }
            }
            return false;
        }

        return false;
    }

    /**
     * Get the balance of a specific currency for a player
     *
     * @param player The player
     * @param currency The currency type
     * @return The balance amount
     */
    public static long getBalance(Player player, Currency currency) {
        if (player == null) return 0;

        String currencyId = currency.getId();

        if (currencyId.equals("coins") || currencyId.equals("dollars")) {
            if (vaultEconomy != null) {
                return (long) vaultEconomy.getBalance(player);
            }
            return 0;
        } else if (currencyId.equals("tokens")) {
            if (tokenManager != null) {
                try {
                    // Using reflection to access TokenManager methods
                    Class<?> managerClass = Class.forName("me.realized.tokenmanager.TokenManagerPlugin");
                    Object tokenManagerInstance = managerClass.cast(tokenManager);

                    // Get the API instance
                    Object api = managerClass.getMethod("getTokenManager").invoke(tokenManagerInstance);
                    Class<?> apiClass = api.getClass();

                    // Get player's tokens
                    long tokens = (long) apiClass.getMethod("getTokens", Player.class).invoke(api, player);
                    return tokens;
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Error getting TokenManager balance", e);
                }
            }
            return 0;
        } else if (currencyId.equals("gems")) {
            if (gemExtension != null) {
                try {
                    // Using reflection to access GemExtension methods
                    Class<?> gemClass = Class.forName("com.blissy.gemextension.GemExtensionPlugin");
                    Object gemInstance = gemClass.cast(gemExtension);

                    // Get player's gems
                    long gems = (long) gemClass.getMethod("getGems", Player.class).invoke(gemInstance, player);
                    return gems;
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Error getting GemExtension balance", e);
                }
            }
            return 0;
        }

        return 0;
    }

    /**
     * Add currency to a player's balance
     *
     * @param player The player
     * @param currency The currency type
     * @param amount The amount to add
     */
    public static void addCurrency(Player player, Currency currency, long amount) {
        if (player == null || amount <= 0) return;

        String currencyId = currency.getId();

        if (currencyId.equals("coins") || currencyId.equals("dollars")) {
            if (vaultEconomy != null) {
                vaultEconomy.depositPlayer(player, amount);
                LOGGER.info("Added " + amount + " " + currency.getName() + " to " + player.getName());
            }
        } else if (currencyId.equals("tokens")) {
            if (tokenManager != null) {
                try {
                    // Using reflection to access TokenManager methods
                    Class<?> managerClass = Class.forName("me.realized.tokenmanager.TokenManagerPlugin");
                    Object tokenManagerInstance = managerClass.cast(tokenManager);

                    // Get the API instance
                    Object api = managerClass.getMethod("getTokenManager").invoke(tokenManagerInstance);
                    Class<?> apiClass = api.getClass();

                    // Add tokens to player
                    apiClass.getMethod("addTokens", Player.class, long.class).invoke(api, player, amount);
                    LOGGER.info("Added " + amount + " " + currency.getName() + " to " + player.getName());
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Error adding TokenManager tokens", e);
                }
            }
        } else if (currencyId.equals("gems")) {
            if (gemExtension != null) {
                try {
                    // Using reflection to access GemExtension methods
                    Class<?> gemClass = Class.forName("com.blissy.gemextension.GemExtensionPlugin");
                    Object gemInstance = gemClass.cast(gemExtension);

                    // Add gems to player
                    gemClass.getMethod("addGems", Player.class, long.class).invoke(gemInstance, player, amount);
                    LOGGER.info("Added " + amount + " " + currency.getName() + " to " + player.getName());
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Error adding GemExtension gems", e);
                }
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
    public static boolean removeCurrency(Player player, Currency currency, long amount) {
        if (player == null || amount <= 0) return false;

        // Check if player has enough first
        if (!hasCurrency(player, currency, amount)) {
            return false;
        }

        String currencyId = currency.getId();

        if (currencyId.equals("coins") || currencyId.equals("dollars")) {
            if (vaultEconomy != null) {
                vaultEconomy.withdrawPlayer(player, amount);
                LOGGER.info("Removed " + amount + " " + currency.getName() + " from " + player.getName());
                return true;
            }
        } else if (currencyId.equals("tokens")) {
            if (tokenManager != null) {
                try {
                    // Using reflection to access TokenManager methods
                    Class<?> managerClass = Class.forName("me.realized.tokenmanager.TokenManagerPlugin");
                    Object tokenManagerInstance = managerClass.cast(tokenManager);

                    // Get the API instance
                    Object api = managerClass.getMethod("getTokenManager").invoke(tokenManagerInstance);
                    Class<?> apiClass = api.getClass();

                    // Remove tokens from player
                    boolean success = (boolean) apiClass.getMethod("removeTokens", Player.class, long.class)
                            .invoke(api, player, amount);

                    if (success) {
                        LOGGER.info("Removed " + amount + " " + currency.getName() + " from " + player.getName());
                    }
                    return success;
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Error removing TokenManager tokens", e);
                }
            }
        } else if (currencyId.equals("gems")) {
            if (gemExtension != null) {
                try {
                    // Using reflection to access GemExtension methods
                    Class<?> gemClass = Class.forName("com.blissy.gemextension.GemExtensionPlugin");
                    Object gemInstance = gemClass.cast(gemExtension);

                    // Remove gems from player
                    boolean success = (boolean) gemClass.getMethod("removeGems", Player.class, long.class)
                            .invoke(gemInstance, player, amount);

                    if (success) {
                        LOGGER.info("Removed " + amount + " " + currency.getName() + " from " + player.getName());
                    }
                    return success;
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Error removing GemExtension gems", e);
                }
            }
        }

        return false;
    }

    /**
     * Format currency amount with proper name and color
     *
     * @param currency The currency type
     * @param amount The amount
     * @param includeColor Whether to include color codes
     * @return Formatted string
     */
    public static String formatCurrency(Currency currency, long amount, boolean includeColor) {
        String colorCode;
        String currencyId = currency.getId();

        if (currencyId.equals("coins") || currencyId.equals("dollars")) {
            colorCode = "§e"; // Yellow
        } else if (currencyId.equals("tokens")) {
            colorCode = "§b"; // Aqua
        } else if (currencyId.equals("gems")) {
            colorCode = "§a"; // Green
        } else {
            colorCode = "§f"; // White
        }

        return includeColor ?
                colorCode + amount + " " + currency.getName() + "§r" :
                amount + " " + currency.getName();
    }
}