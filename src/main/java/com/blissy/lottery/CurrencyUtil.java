package com.blissy.lottery;

import com.blissy.lottery.currency.Currency;
import com.pixelmonmod.pixelmon.Pixelmon;
import net.minecraft.entity.player.ServerPlayerEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Utility class for managing currencies across different economy plugins.
 * This class provides a bridge between Forge/Pixelmon entities and Bukkit plugins.
 */
public class CurrencyUtil {
    private static final Logger LOGGER = LogManager.getLogger();
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
            LOGGER.error("Failed to initialize economy hooks", e);
        }
    }

    /**
     * Convert a Forge/Pixelmon player to a Bukkit player.
     * This is necessary for interacting with Bukkit plugins.
     *
     * @param forgePlayer The Forge player entity
     * @return The equivalent Bukkit player, or null if not found
     */
    private static Player getBukkitPlayer(ServerPlayerEntity forgePlayer) {
        if (forgePlayer == null) return null;

        try {
            // Get the player by UUID
            return Bukkit.getPlayer(forgePlayer.getUUID());
        } catch (Exception e) {
            LOGGER.error("Failed to get Bukkit player for {}", forgePlayer.getName().getString(), e);
            return null;
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
    public static boolean hasCurrency(ServerPlayerEntity player, Currency currency, int amount) {
        if (player == null) return false;

        Player bukkitPlayer = getBukkitPlayer(player);
        if (bukkitPlayer == null) return false;

        return switch (currency) {
            case SERVER_CURRENCY -> {
                if (vaultEconomy != null) {
                    yield vaultEconomy.has(bukkitPlayer, amount);
                }
                yield false;
            }

            case TOKENS -> {
                if (tokenManager != null) {
                    try {
                        // Using reflection to access TokenManager methods
                        Class<?> managerClass = Class.forName("me.realized.tokenmanager.TokenManagerPlugin");
                        Object tokenManagerInstance = managerClass.cast(tokenManager);

                        // Get the API instance
                        Object api = managerClass.getMethod("getTokenManager").invoke(tokenManagerInstance);
                        Class<?> apiClass = api.getClass();

                        // Get player's tokens
                        long tokens = (long) apiClass.getMethod("getTokens", Player.class).invoke(api, bukkitPlayer);
                        yield tokens >= amount;
                    } catch (Exception e) {
                        LOGGER.error("Error checking TokenManager balance", e);
                    }
                }
                yield false;
            }

            case GEMS -> {
                if (gemExtension != null) {
                    try {
                        // Using reflection to access GemExtension methods
                        Class<?> gemClass = Class.forName("com.blissy.gemextension.GemExtensionPlugin");
                        Object gemInstance = gemClass.cast(gemExtension);

                        // Get player's gems
                        long gems = (long) gemClass.getMethod("getGems", Player.class).invoke(gemInstance, bukkitPlayer);
                        yield gems >= amount;
                    } catch (Exception e) {
                        LOGGER.error("Error checking GemExtension balance", e);
                    }
                }
                yield false;
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

        Player bukkitPlayer = getBukkitPlayer(player);
        if (bukkitPlayer == null) return 0;

        return switch (currency) {
            case SERVER_CURRENCY -> {
                if (vaultEconomy != null) {
                    yield (int) vaultEconomy.getBalance(bukkitPlayer);
                }
                yield 0;
            }

            case TOKENS -> {
                if (tokenManager != null) {
                    try {
                        // Using reflection to access TokenManager methods
                        Class<?> managerClass = Class.forName("me.realized.tokenmanager.TokenManagerPlugin");
                        Object tokenManagerInstance = managerClass.cast(tokenManager);

                        // Get the API instance
                        Object api = managerClass.getMethod("getTokenManager").invoke(tokenManagerInstance);
                        Class<?> apiClass = api.getClass();

                        // Get player's tokens
                        long tokens = (long) apiClass.getMethod("getTokens", Player.class).invoke(api, bukkitPlayer);
                        return (int) tokens;
                    } catch (Exception e) {
                        LOGGER.error("Error getting TokenManager balance", e);
                    }
                }
                yield 0;
            }

            case GEMS -> {
                if (gemExtension != null) {
                    try {
                        // Using reflection to access GemExtension methods
                        Class<?> gemClass = Class.forName("com.blissy.gemextension.GemExtensionPlugin");
                        Object gemInstance = gemClass.cast(gemExtension);

                        // Get player's gems
                        long gems = (long) gemClass.getMethod("getGems", Player.class).invoke(gemInstance, bukkitPlayer);
                        return (int) gems;
                    } catch (Exception e) {
                        LOGGER.error("Error getting GemExtension balance", e);
                    }
                }
                yield 0;
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

        Player bukkitPlayer = getBukkitPlayer(player);
        if (bukkitPlayer == null) return;

        switch (currency) {
            case SERVER_CURRENCY -> {
                if (vaultEconomy != null) {
                    vaultEconomy.depositPlayer(bukkitPlayer, amount);
                    LOGGER.info("Added {} {} to {}", amount, currency.getName(), player.getName().getString());
                }
            }

            case TOKENS -> {
                if (tokenManager != null) {
                    try {
                        // Using reflection to access TokenManager methods
                        Class<?> managerClass = Class.forName("me.realized.tokenmanager.TokenManagerPlugin");
                        Object tokenManagerInstance = managerClass.cast(tokenManager);

                        // Get the API instance
                        Object api = managerClass.getMethod("getTokenManager").invoke(tokenManagerInstance);
                        Class<?> apiClass = api.getClass();

                        // Add tokens to player
                        apiClass.getMethod("addTokens", Player.class, long.class).invoke(api, bukkitPlayer, (long) amount);
                        LOGGER.info("Added {} {} to {}", amount, currency.getName(), player.getName().getString());
                    } catch (Exception e) {
                        LOGGER.error("Error adding TokenManager tokens", e);
                    }
                }
            }

            case GEMS -> {
                if (gemExtension != null) {
                    try {
                        // Using reflection to access GemExtension methods
                        Class<?> gemClass = Class.forName("com.blissy.gemextension.GemExtensionPlugin");
                        Object gemInstance = gemClass.cast(gemExtension);

                        // Add gems to player
                        gemClass.getMethod("addGems", Player.class, long.class).invoke(gemInstance, bukkitPlayer, (long) amount);
                        LOGGER.info("Added {} {} to {}", amount, currency.getName(), player.getName().getString());
                    } catch (Exception e) {
                        LOGGER.error("Error adding GemExtension gems", e);
                    }
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
    public static boolean removeCurrency(ServerPlayerEntity player, Currency currency, int amount) {
        if (player == null || amount <= 0) return false;

        // Check if player has enough first
        if (!hasCurrency(player, currency, amount)) {
            return false;
        }

        Player bukkitPlayer = getBukkitPlayer(player);
        if (bukkitPlayer == null) return false;

        switch (currency) {
            case SERVER_CURRENCY -> {
                if (vaultEconomy != null) {
                    vaultEconomy.withdrawPlayer(bukkitPlayer, amount);
                    LOGGER.info("Removed {} {} from {}", amount, currency.getName(), player.getName().getString());
                    return true;
                }
            }

            case TOKENS -> {
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
                                .invoke(api, bukkitPlayer, (long) amount);

                        if (success) {
                            LOGGER.info("Removed {} {} from {}", amount, currency.getName(), player.getName().getString());
                        }
                        return success;
                    } catch (Exception e) {
                        LOGGER.error("Error removing TokenManager tokens", e);
                    }
                }
            }

            case GEMS -> {
                if (gemExtension != null) {
                    try {
                        // Using reflection to access GemExtension methods
                        Class<?> gemClass = Class.forName("com.blissy.gemextension.GemExtensionPlugin");
                        Object gemInstance = gemClass.cast(gemExtension);

                        // Remove gems from player
                        boolean success = (boolean) gemClass.getMethod("removeGems", Player.class, long.class)
                                .invoke(gemInstance, bukkitPlayer, (long) amount);

                        if (success) {
                            LOGGER.info("Removed {} {} from {}", amount, currency.getName(), player.getName().getString());
                        }
                        return success;
                    } catch (Exception e) {
                        LOGGER.error("Error removing GemExtension gems", e);
                    }
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
    public static String formatCurrency(Currency currency, int amount, boolean includeColor) {
        String colorCode = switch (currency) {
            case SERVER_CURRENCY -> "§e"; // Yellow
            case TOKENS -> "§b";    // Aqua
            case GEMS -> "§a";      // Green
        };

        return includeColor ?
                colorCode + amount + " " + currency.getName() + "§r" :
                amount + " " + currency.getName();
    }
}