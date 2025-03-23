package com.blissy.lottery.managers;

import com.blissy.lottery.BlissyLottery;
import com.blissy.lottery.currency.Currency;
import com.blissy.lottery.utils.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.logging.Level;

public class LotteryManager {
    private final BlissyLottery plugin;
    private final Map<String, Map<UUID, Long>> entries = new HashMap<>();
    private LocalDateTime nextDrawingTime;
    private BukkitTask drawingTask;
    private BukkitTask notificationTask;

    private final File dataFile;
    private final File offlineRewardsFolder;

    // Constants
    private static final long TICKS_PER_MINUTE = 20 * 60;
    private static final long NOTIFICATION_INTERVAL = 30; // minutes

    public LotteryManager(BlissyLottery plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "lottery_data.yml");
        this.offlineRewardsFolder = new File(plugin.getDataFolder(), "offline");

        // Initialize entries map for all currencies
        for (Currency currency : plugin.getCurrencyManager().getAllCurrencies()) {
            entries.put(currency.getId(), new HashMap<>());
        }

        // Set default next drawing time
        nextDrawingTime = LocalDateTime.now()
                .with(TemporalAdjusters.next(DayOfWeek.SUNDAY))
                .withHour(0)
                .withMinute(0)
                .withSecond(0);
    }

    /**
     * Start the lottery scheduler tasks.
     */
    public void startScheduler() {
        // Calculate ticks until next drawing
        long ticksUntilDrawing = TimeUtil.getTicksUntil(nextDrawingTime);

        // Schedule the drawing task
        drawingTask = Bukkit.getScheduler().runTaskLater(plugin, this::performDrawing, ticksUntilDrawing);

        // Schedule periodic notifications
        notificationTask = Bukkit.getScheduler().runTaskTimer(
                plugin,
                this::broadcastLotteryStatus,
                TICKS_PER_MINUTE * 5, // 5 minutes delay
                TICKS_PER_MINUTE * NOTIFICATION_INTERVAL // Every 30 minutes
        );

        plugin.getLogger().info("Lottery drawing scheduled for " +
                DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy 'at' HH:mm").format(nextDrawingTime));
    }

    /**
     * Shut down the scheduler tasks.
     */
    public void stopScheduler() {
        if (drawingTask != null) {
            drawingTask.cancel();
        }

        if (notificationTask != null) {
            notificationTask.cancel();
        }
    }

    /**
     * Reload the scheduler (used after config changes).
     */
    public void reloadScheduler() {
        stopScheduler();
        startScheduler();
    }

    /**
     * Add a lottery entry for a player.
     * @param currency The currency being used
     * @param player The player
     * @param amount The amount of currency
     * @return True if successful, false otherwise
     */
    public boolean addEntry(Currency currency, Player player, long amount) {
        // Check if player has enough currency
        if (!currency.hasBalance(player, amount)) {
            player.sendMessage(ChatColor.RED + "You don't have enough " + currency.getName() + "!");
            return false;
        }

        // Withdraw the currency
        if (!currency.withdraw(player, amount)) {
            player.sendMessage(ChatColor.RED + "Failed to withdraw " + currency.formatAmount(amount) + "!");
            return false;
        }

        // Add entry
        Map<UUID, Long> currencyEntries = entries.get(currency.getId());
        UUID playerId = player.getUniqueId();
        currencyEntries.put(playerId, currencyEntries.getOrDefault(playerId, 0L) + amount);

        // Notify player
        player.sendMessage(ChatColor.GREEN + "You have entered the " + currency.getColoredName() +
                ChatColor.GREEN + " lottery with " + currency.formatAmount(amount) + "!");

        // Save data
        saveData();

        return true;
    }

    /**
     * Get a player's entries for a specific currency.
     * @param currency The currency
     * @param playerId The player UUID
     * @return The amount of entries
     */
    public long getPlayerEntries(Currency currency, UUID playerId) {
        Map<UUID, Long> currencyEntries = entries.get(currency.getId());
        return currencyEntries != null ? currencyEntries.getOrDefault(playerId, 0L) : 0L;
    }

    /**
     * Get all entries for a specific currency.
     * @param currency The currency
     * @return Map of player UUIDs to entry amounts
     */
    public Map<UUID, Long> getAllEntries(Currency currency) {
        return entries.getOrDefault(currency.getId(), new HashMap<>());
    }

    /**
     * Get the total pool amount for a currency.
     * @param currency The currency
     * @return The total pool amount
     */
    public long getPoolTotal(Currency currency) {
        Map<UUID, Long> currencyEntries = entries.get(currency.getId());
        return currencyEntries.values().stream().mapToLong(Long::longValue).sum();
    }

    /**
     * Get the number of participants for a currency.
     * @param currency The currency
     * @return The number of participants
     */
    public int getParticipantCount(Currency currency) {
        Map<UUID, Long> currencyEntries = entries.get(currency.getId());
        return currencyEntries.size();
    }

    /**
     * Get the next drawing time.
     * @return The next drawing time
     */
    public LocalDateTime getNextDrawingTime() {
        return nextDrawingTime;
    }

    /**
     * Set the next drawing time.
     * @param nextDrawingTime The new drawing time
     */
    public void setNextDrawingTime(LocalDateTime nextDrawingTime) {
        this.nextDrawingTime = nextDrawingTime;

        // Reschedule the drawing task
        if (drawingTask != null) {
            drawingTask.cancel();
        }

        long ticksUntilDrawing = TimeUtil.getTicksUntil(nextDrawingTime);
        drawingTask = Bukkit.getScheduler().runTaskLater(plugin, this::performDrawing, ticksUntilDrawing);
    }

    /**
     * Broadcast lottery status to all online players.
     */
    public void broadcastLotteryStatus() {
        String timeRemaining = TimeUtil.formatTimeUntil(nextDrawingTime);
        String nextDrawingStr = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy 'at' HH:mm").format(nextDrawingTime);

        Bukkit.broadcastMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "===== Weekly Lottery Status =====");

        for (Currency currency : plugin.getCurrencyManager().getAllCurrencies()) {
            long poolTotal = getPoolTotal(currency);
            int participants = getParticipantCount(currency);

            Bukkit.broadcastMessage(ChatColor.GOLD + currency.getName() + " Pool: " +
                    currency.getColoredName() + " " + currency.formatAmount(poolTotal) +
                    ChatColor.GRAY + " (" + participants + " participants)");
        }

        Bukkit.broadcastMessage(ChatColor.GOLD + "Next drawing: " + ChatColor.WHITE + nextDrawingStr +
                ChatColor.GRAY + " (" + timeRemaining + " remaining)");

        Bukkit.broadcastMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "===========================");
    }

    /**
     * Perform the lottery drawing.
     */
    private void performDrawing() {
        plugin.getLogger().info("Performing lottery drawing...");

        // Announce the drawing
        Bukkit.broadcastMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "[LOTTERY] " +
                ChatColor.YELLOW + "The weekly lottery drawing is now taking place!");

        // Process each currency
        for (Currency currency : plugin.getCurrencyManager().getAllCurrencies()) {
            Map<UUID, Long> currencyEntries = entries.get(currency.getId());

            if (currencyEntries.isEmpty()) {
                Bukkit.broadcastMessage(ChatColor.GOLD + "[Lottery] " + ChatColor.WHITE +
                        "No entries were made for the " + currency.getName() + " lottery this week.");
                continue;
            }

            // Calculate total pool
            long poolTotal = getPoolTotal(currency);

            // Create weighted entries list
            List<UUID> weightedEntries = new ArrayList<>();
            for (Map.Entry<UUID, Long> entry : currencyEntries.entrySet()) {
                for (int i = 0; i < entry.getValue(); i++) {
                    weightedEntries.add(entry.getKey());
                }
            }

            // Select random winner
            UUID winnerId = weightedEntries.get(new Random().nextInt(weightedEntries.size()));

            // Get winner name
            String winnerName = "Unknown";
            Player winner = Bukkit.getPlayer(winnerId);
            if (winner != null) {
                winnerName = winner.getName();
            } else {
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(winnerId);
                if (offlinePlayer.hasPlayedBefore()) {
                    winnerName = offlinePlayer.getName();
                }
            }

            // Announce winner
            Bukkit.broadcastMessage(ChatColor.GOLD + "[Lottery] " + ChatColor.WHITE +
                    winnerName + " has won the " + currency.getColoredName() +
                    ChatColor.WHITE + " lottery! Prize: " + currency.formatAmount(poolTotal) + "!");

            // Award prize
            if (winner != null) {
                // Online player
                currency.deposit(winner, poolTotal);
                winner.sendMessage(ChatColor.GOLD + "[Lottery] " + ChatColor.GREEN +
                        "Congratulations! You won the " + currency.getColoredName() +
                        ChatColor.GREEN + " lottery! Your prize of " + currency.formatAmount(poolTotal) +
                        " has been added to your account.");
            } else {
                // Offline player
                storeOfflineReward(winnerId, currency, poolTotal);
            }

            // Clear entries for this currency
            currencyEntries.clear();
        }

        // Set next drawing time to next Sunday
        nextDrawingTime = LocalDateTime.now()
                .with(TemporalAdjusters.next(DayOfWeek.SUNDAY))
                .withHour(0)
                .withMinute(0)
                .withSecond(0);

        // Announce next drawing
        Bukkit.broadcastMessage(ChatColor.GOLD + "[Lottery] " + ChatColor.WHITE +
                "The next lottery drawing will take place on " +
                DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy 'at' HH:mm").format(nextDrawingTime));

        // Reschedule the drawing task
        long ticksUntilDrawing = TimeUtil.getTicksUntil(nextDrawingTime);
        drawingTask = Bukkit.getScheduler().runTaskLater(plugin, this::performDrawing, ticksUntilDrawing);

        // Save data
        saveData();
    }

    /**
     * Store a reward for an offline player.
     * @param playerId The player UUID
     * @param currency The currency
     * @param amount The amount
     */
    private void storeOfflineReward(UUID playerId, Currency currency, long amount) {
        try {
            File rewardFile = new File(offlineRewardsFolder, playerId.toString() + ".yml");
            YamlConfiguration config = rewardFile.exists()
                    ? YamlConfiguration.loadConfiguration(rewardFile)
                    : new YamlConfiguration();

            int rewardId = config.getKeys(false).size() + 1;
            String path = String.valueOf(rewardId);

            config.set(path + ".currency", currency.getId());
            config.set(path + ".amount", amount);
            config.set(path + ".timestamp", System.currentTimeMillis());

            config.save(rewardFile);

            plugin.getLogger().info("Stored offline reward for player " + playerId +
                    ": " + currency.formatAmount(amount));
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to store offline reward", e);
        }
    }

    /**
     * Process offline rewards for a player.
     * @param player The player
     */
    public void processOfflineRewards(Player player) {
        File rewardFile = new File(offlineRewardsFolder, player.getUniqueId().toString() + ".yml");
        if (!rewardFile.exists()) {
            return;
        }

        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(rewardFile);

            for (String key : config.getKeys(false)) {
                String currencyId = config.getString(key + ".currency");
                long amount = config.getLong(key + ".amount");
                long timestamp = config.getLong(key + ".timestamp");

                Optional<Currency> currency = plugin.getCurrencyManager().getCurrency(currencyId);
                if (currency.isPresent()) {
                    // Award the prize
                    currency.get().deposit(player, amount);

                    // Format timestamp
                    Date date = new Date(timestamp);
                    String timeStr = new java.text.SimpleDateFormat("MMMM d").format(date);

                    // Notify player
                    player.sendMessage(ChatColor.GOLD + "[Lottery] " + ChatColor.WHITE +
                            "While you were away, you won the " + currency.get().getColoredName() +
                            ChatColor.WHITE + " lottery on " + timeStr + "! Your prize of " +
                            currency.get().formatAmount(amount) + " has been added to your account.");
                }
            }

            // Delete the reward file
            rewardFile.delete();

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to process offline rewards for " +
                    player.getName(), e);
        }
    }

    /**
     * Load lottery data from file.
     */
    public void loadData() {
        if (!dataFile.exists()) {
            return;
        }

        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);

            // Load next drawing time
            String timeStr = config.getString("next_drawing");
            if (timeStr != null) {
                nextDrawingTime = LocalDateTime.parse(timeStr);
            }

            // Load entries
            ConfigurationSection entriesSection = config.getConfigurationSection("entries");
            if (entriesSection != null) {
                for (String currencyId : entriesSection.getKeys(false)) {
                    Map<UUID, Long> currencyEntries = new HashMap<>();
                    entries.put(currencyId, currencyEntries);

                    ConfigurationSection currencySection = entriesSection.getConfigurationSection(currencyId);
                    if (currencySection != null) {
                        for (String playerIdStr : currencySection.getKeys(false)) {
                            try {
                                UUID playerId = UUID.fromString(playerIdStr);
                                long amount = currencySection.getLong(playerIdStr);
                                currencyEntries.put(playerId, amount);
                            } catch (IllegalArgumentException e) {
                                plugin.getLogger().warning("Invalid UUID in lottery data: " + playerIdStr);
                            }
                        }
                    }
                }
            }

            plugin.getLogger().info("Loaded lottery data successfully");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load lottery data", e);
        }
    }

    /**
     * Save lottery data to file.
     */
    public void saveData() {
        try {
            YamlConfiguration config = new YamlConfiguration();

            // Save next drawing time
            config.set("next_drawing", nextDrawingTime.toString());

            // Save entries
            for (Map.Entry<String, Map<UUID, Long>> entry : entries.entrySet()) {
                String currencyId = entry.getKey();
                Map<UUID, Long> currencyEntries = entry.getValue();

                for (Map.Entry<UUID, Long> playerEntry : currencyEntries.entrySet()) {
                    config.set("entries." + currencyId + "." + playerEntry.getKey().toString(),
                            playerEntry.getValue());
                }
            }

            config.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save lottery data", e);
        }
    }
}