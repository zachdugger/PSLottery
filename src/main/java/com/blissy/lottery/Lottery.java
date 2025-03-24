package com.blissy.lottery;

import com.blissy.lottery.commands.LotteryCommand;
import com.blissy.lottery.currency.CurrencyManager;
import com.blissy.lottery.currency.GemCurrency;
import com.blissy.lottery.currency.TokenCurrency;
import com.blissy.lottery.currency.VaultCurrency;
import com.blissy.lottery.listeners.PlayerListener;
import com.blissy.lottery.managers.LotteryManager;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.logging.Level;

/**
 * Main class for the Lottery plugin.
 */
public class Lottery extends JavaPlugin {
    private static Lottery instance;
    private CurrencyManager currencyManager;
    private LotteryManager lotteryManager;
    private net.milkbowl.vault.economy.Economy vaultEconomy;
    private me.realized.tokenmanager.TokenManagerPlugin tokenManager;
    private com.blissy.gemextension.GemExtensionPlugin gemExtension;
    private PlayerListener playerListener;

    @Override
    public void onEnable() {
        instance = this;

        // Debug plugin.yml location
        File pluginFile = new File(getDataFolder().getParentFile(), "PSLottery-1.0.0.jar");
        getLogger().info("Plugin JAR exists: " + pluginFile.exists());

        // Save default config
        saveDefaultConfig();

        // Setup folder structure
        setupFolders();

        // Hook into dependencies
        if (!setupDependencies()) {
            getLogger().warning("Failed to hook into one or more required dependencies! Some features may not work.");
        }

        // Initialize currency manager
        currencyManager = new CurrencyManager();
        setupCurrencies();

        // Initialize lottery manager
        lotteryManager = new LotteryManager(this);
        lotteryManager.loadData();
        lotteryManager.startScheduler();

        // Get and check the command
        PluginCommand command = getCommand("lottery");
        if (command == null) {
            getLogger().severe("Failed to get lottery command! The plugin.yml might not be loaded correctly.");
            getLogger().severe("Contents of plugin.yml in JAR might be incorrect or missing.");
            getLogger().severe("Commands registered with server: " +
                    Bukkit.getPluginManager().getPlugin("PSLottery").getDescription().getCommands().keySet());

            try {
                getLogger().info("Commands registered with this plugin: " +
                        getDescription().getCommands().keySet());
            } catch (Exception e) {
                getLogger().severe("Could not access commands: " + e.getMessage());
            }
        } else {
            // Register command executor
            getLogger().info("Registering command executor for /lottery");
            LotteryCommand lotteryCmd = new LotteryCommand(this);
            command.setExecutor(lotteryCmd);
            command.setTabCompleter(lotteryCmd);
            getLogger().info("Command executor registered successfully!");
        }

        // Register listeners
        playerListener = new PlayerListener(this);
        getServer().getPluginManager().registerEvents(playerListener, this);

        getLogger().info("Lottery has been enabled!");

        // Schedule first pool notification
        Bukkit.getScheduler().runTaskLater(this, () ->
                        lotteryManager.broadcastLotteryStatus(),
                20 * 30); // 30 seconds after startup
    }

    @Override
    public void onDisable() {
        if (lotteryManager != null) {
            lotteryManager.saveData();
            lotteryManager.stopScheduler();
        }

        getLogger().info("Lottery has been disabled!");
    }

    /**
     * Reload the plugin configuration and data
     * @return True if reload was successful, false otherwise
     */
    public boolean reload() {
        getLogger().info("Reloading PSLottery...");

        // Save current lottery data before reload
        if (lotteryManager != null) {
            lotteryManager.saveData();
            lotteryManager.stopScheduler();
        }

        try {
            // Reload config
            reloadConfig();
            getLogger().info("Configuration reloaded");

            // Reinitialize currency manager
            currencyManager = new CurrencyManager();
            setupCurrencies();
            getLogger().info("Currencies reloaded");

            // Reinitialize lottery manager
            lotteryManager = new LotteryManager(this);
            lotteryManager.loadData();
            lotteryManager.startScheduler();
            getLogger().info("Lottery manager reloaded");

            // Schedule status notification
            Bukkit.getScheduler().runTaskLater(this, () ->
                            lotteryManager.broadcastLotteryStatus(),
                    20 * 5); // 5 seconds after reload

            getLogger().info("PSLottery reload complete!");
            return true;
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error reloading plugin", e);
            return false;
        }
    }

    private void setupFolders() {
        // Create data folder if it doesn't exist
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }

        // Create offline rewards folder
        File offlineFolder = new File(getDataFolder(), "offline");
        if (!offlineFolder.exists()) {
            offlineFolder.mkdir();
        }
    }

    private boolean setupDependencies() {
        boolean success = true;

        // Setup Vault Economy
        if (getServer().getPluginManager().getPlugin("Vault") != null) {
            RegisteredServiceProvider<net.milkbowl.vault.economy.Economy> rsp =
                    getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);

            if (rsp != null) {
                vaultEconomy = rsp.getProvider();
                getLogger().info("Successfully hooked into Vault economy!");
            } else {
                getLogger().warning("Failed to hook into Vault economy!");
                success = false;
            }
        } else {
            getLogger().warning("Vault not found! Server currency will not be available.");
            success = false;
        }

        // Setup TokenManager
        if (getServer().getPluginManager().getPlugin("TokenManager") != null) {
            try {
                tokenManager = (me.realized.tokenmanager.TokenManagerPlugin) getServer().getPluginManager().getPlugin("TokenManager");
                getLogger().info("Successfully hooked into TokenManager!");
            } catch (Exception e) {
                getLogger().log(Level.WARNING, "Failed to hook into TokenManager!", e);
                success = false;
            }
        } else {
            getLogger().warning("TokenManager not found! Token currency will not be available.");
            success = false;
        }

        // Setup GemExtension
        if (getServer().getPluginManager().getPlugin("GemExtension") != null) {
            try {
                gemExtension = (com.blissy.gemextension.GemExtensionPlugin) getServer().getPluginManager().getPlugin("GemExtension");
                getLogger().info("Successfully hooked into GemExtension!");
            } catch (Exception e) {
                getLogger().log(Level.WARNING, "Failed to hook into GemExtension!", e);
                success = false;
            }
        } else {
            getLogger().warning("GemExtension not found! Gem currency will not be available.");
            success = false;
        }

        return success;
    }

    private void setupCurrencies() {
        FileConfiguration config = getConfig();

        // Load enabled currencies from config
        if (vaultEconomy != null && config.getBoolean("currencies.coins.enabled", true)) {
            currencyManager.registerCurrency(new VaultCurrency(vaultEconomy));
            getLogger().info("Registered Vault currency: " + vaultEconomy.currencyNamePlural());
        }

        if (tokenManager != null && config.getBoolean("currencies.tokens.enabled", true)) {
            currencyManager.registerCurrency(new TokenCurrency(tokenManager));
            getLogger().info("Registered Token currency");
        }

        if (gemExtension != null && config.getBoolean("currencies.gems.enabled", true)) {
            currencyManager.registerCurrency(new GemCurrency(gemExtension));
            getLogger().info("Registered Gem currency");
        }
    }

    /**
     * Get the instance of the plugin.
     * @return The plugin instance
     */
    public static Lottery getInstance() {
        return instance;
    }

    /**
     * Get the currency manager.
     * @return The currency manager
     */
    public CurrencyManager getCurrencyManager() {
        return currencyManager;
    }

    /**
     * Get the lottery manager.
     * @return The lottery manager
     */
    public LotteryManager getLotteryManager() {
        return lotteryManager;
    }

    /**
     * Get the Vault economy.
     * @return The Vault economy
     */
    public net.milkbowl.vault.economy.Economy getVaultEconomy() {
        return vaultEconomy;
    }

    /**
     * Get the TokenManager plugin.
     * @return The TokenManager plugin
     */
    public me.realized.tokenmanager.TokenManagerPlugin getTokenManager() {
        return tokenManager;
    }

    /**
     * Get the GemExtension plugin.
     * @return The GemExtension plugin
     */
    public com.blissy.gemextension.GemExtensionPlugin getGemExtension() {
        return gemExtension;
    }
}