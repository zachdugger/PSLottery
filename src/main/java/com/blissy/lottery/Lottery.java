package com.blissy.lottery;

import com.blissy.lottery.commands.LotteryCommand;
import com.blissy.lottery.currency.CurrencyManager;
import com.blissy.lottery.currency.GemCurrency;
import com.blissy.lottery.currency.TokenCurrency;
import com.blissy.lottery.currency.VaultCurrency;
import com.blissy.lottery.listeners.PlayerListener;
import com.blissy.lottery.managers.LotteryManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.logging.Level;

/**
 * Main class for the BlissyLottery plugin.
 */
public class Lottery extends JavaPlugin {
    private static Lottery instance;
    private CurrencyManager currencyManager;
    private LotteryManager lotteryManager;
    private net.milkbowl.vault.economy.Economy vaultEconomy;
    private me.realized.tokenmanager.TokenManagerPlugin tokenManager;
    private com.blissy.gemextension.GemExtensionPlugin gemExtension;

    @Override
    public void onEnable() {
        instance = this;

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

        // Register commands
        getCommand("lottery").setExecutor(new LotteryCommand(this));

        // Register listeners
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

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
        }

        getLogger().info("Lottery has been disabled!");
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