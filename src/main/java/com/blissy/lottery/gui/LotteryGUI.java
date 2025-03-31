package com.blissy.lottery.gui;

import com.blissy.lottery.Lottery;
import com.blissy.lottery.currency.Currency;
import com.blissy.lottery.utils.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LotteryGUI implements Listener {
    private final Lottery plugin;
    private final Map<Player, GUIState> playerStates = new HashMap<>();

    public LotteryGUI(Lottery plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Open the main lottery menu.
     * @param player The player
     */
    public void openMainMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 27, ChatColor.GOLD + "Weekly Lottery");

        // Add info item
        ItemStack infoItem = createItem(Material.CLOCK,
                ChatColor.GOLD + "Lottery Information",
                ChatColor.YELLOW + "Next drawing: " + ChatColor.WHITE +
                        DateTimeFormatter.ofPattern("EEEE, MMMM d")
                                .format(plugin.getLotteryManager().getNextDrawingTime()),
                ChatColor.YELLOW + "Time remaining: " + ChatColor.WHITE +
                        TimeUtil.formatTimeUntil(plugin.getLotteryManager().getNextDrawingTime()));
        inventory.setItem(4, infoItem);

        // Add currency items
        int slot = 10;
        for (Currency currency : plugin.getCurrencyManager().getAllCurrencies()) {
            long poolTotal = plugin.getLotteryManager().getPoolTotal(currency);
            int participants = plugin.getLotteryManager().getParticipantCount(currency);
            long playerEntries = plugin.getLotteryManager().getPlayerEntries(currency, player.getUniqueId());

            Material material = getMaterialForCurrency(currency);

            ItemStack currencyItem = createItem(material,
                    currency.getColoredName() + ChatColor.GOLD + " Lottery",
                    ChatColor.YELLOW + "Current pool: " + ChatColor.WHITE + currency.formatAmount(poolTotal),
                    ChatColor.YELLOW + "Participants: " + ChatColor.WHITE + participants,
                    ChatColor.YELLOW + "Your entries: " + ChatColor.WHITE + playerEntries,
                    ChatColor.YELLOW + "Your balance: " + ChatColor.WHITE + currency.formatAmount(currency.getBalance(player)),
                    "",
                    ChatColor.GREEN + "Click to enter this lottery");

            inventory.setItem(slot, currencyItem);
            slot += 2;
        }

        player.openInventory(inventory);
        playerStates.put(player, new GUIState(GUIType.MAIN_MENU));
    }

    /**
     * Open the entry menu for a specific currency.
     * @param player The player
     * @param currency The currency
     */
    private void openEntryMenu(Player player, Currency currency) {
        Inventory inventory = Bukkit.createInventory(null, 27,
                ChatColor.GOLD + "Enter " + currency.getName() + " Lottery");

        // Add currency info
        long poolTotal = plugin.getLotteryManager().getPoolTotal(currency);
        int participants = plugin.getLotteryManager().getParticipantCount(currency);
        long playerEntries = plugin.getLotteryManager().getPlayerEntries(currency, player.getUniqueId());
        long playerBalance = currency.getBalance(player);

        ItemStack infoItem = createItem(getMaterialForCurrency(currency),
                currency.getColoredName() + ChatColor.GOLD + " Lottery",
                ChatColor.YELLOW + "Current pool: " + ChatColor.WHITE + currency.formatAmount(poolTotal),
                ChatColor.YELLOW + "Participants: " + ChatColor.WHITE + participants,
                ChatColor.YELLOW + "Your entries: " + ChatColor.WHITE + playerEntries,
                ChatColor.YELLOW + "Your balance: " + ChatColor.WHITE + currency.formatAmount(playerBalance));
        inventory.setItem(4, infoItem);

        // Add entry options
        long[] amounts = getEntryAmounts(playerBalance);
        int slot = 10;

        for (long amount : amounts) {
            if (amount <= playerBalance) {
                ItemStack amountItem = createItem(Material.GOLD_INGOT,
                        ChatColor.GOLD + "Enter with " + currency.formatAmount(amount),
                        ChatColor.YELLOW + "Click to enter the lottery with " + currency.formatAmount(amount));
                inventory.setItem(slot, amountItem);
            }
            slot++;
        }

        // Add back button
        ItemStack backButton = createItem(Material.ARROW,
                ChatColor.RED + "Back to Main Menu",
                ChatColor.GRAY + "Click to return to the main lottery menu");
        inventory.setItem(22, backButton);

        player.openInventory(inventory);
        playerStates.put(player, new GUIState(GUIType.ENTRY_MENU, currency));
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        GUIState state = playerStates.get(player);

        if (state == null) {
            return;
        }

        event.setCancelled(true);

        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) {
            return;
        }

        // Handle based on GUI type
        if (state.getType() == GUIType.MAIN_MENU) {
            handleMainMenuClick(player, event.getSlot());
        } else if (state.getType() == GUIType.ENTRY_MENU) {
            handleEntryMenuClick(player, event.getSlot(), state.getCurrency());
        }
    }

    private void handleMainMenuClick(Player player, int slot) {
        // Currency entries are at slots 10, 12, 14, 16
        if (slot == 10 || slot == 12 || slot == 14 || slot == 16) {
            int index = (slot - 10) / 2;
            List<Currency> currencies = new ArrayList<>(plugin.getCurrencyManager().getAllCurrencies());

            if (index < currencies.size()) {
                Currency currency = currencies.get(index);
                openEntryMenu(player, currency);
            }
        }
    }

    private void handleEntryMenuClick(Player player, int slot, Currency currency) {
        // Back button is at slot 22
        if (slot == 22) {
            openMainMenu(player);
            return;
        }

        // Entry options are at slots 10-15
        if (slot >= 10 && slot <= 15) {
            long[] amounts = getEntryAmounts(currency.getBalance(player));
            int index = slot - 10;

            if (index < amounts.length) {
                long amount = amounts[index];

                if (amount <= currency.getBalance(player)) {
                    player.closeInventory();

                    // Add entry
                    boolean success = plugin.getLotteryManager().addEntry(currency, player, amount);


            }
        }
    }}

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player) {
            playerStates.remove((Player) event.getPlayer());
        }
    }

    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(name);

            if (lore.length > 0) {
                List<String> loreList = new ArrayList<>();
                for (String line : lore) {
                    loreList.add(line);
                }
                meta.setLore(loreList);
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    private Material getMaterialForCurrency(Currency currency) {
        String id = currency.getId();

        if (id.contains("coin") || id.equalsIgnoreCase("vault")) {
            return Material.GOLD_INGOT;
        } else if (id.contains("token")) {
            return Material.EMERALD;
        } else if (id.contains("gem")) {
            return Material.DIAMOND;
        }

        return Material.PAPER;
    }

    private long[] getEntryAmounts(long playerBalance) {
        // Calculate reasonable entry amounts based on player balance
        if (playerBalance < 100) {
            return new long[]{1, 5, 10, 25, 50, playerBalance};
        } else if (playerBalance < 1000) {
            return new long[]{10, 50, 100, 250, 500, playerBalance};
        } else if (playerBalance < 10000) {
            return new long[]{100, 500, 1000, 2500, 5000, playerBalance};
        } else {
            return new long[]{1000, 5000, 10000, 25000, 50000, playerBalance};
        }
    }

    private enum GUIType {
        MAIN_MENU,
        ENTRY_MENU
    }

    private static class GUIState {
        private final GUIType type;
        private final Currency currency;

        public GUIState(GUIType type) {
            this(type, null);
        }

        public GUIState(GUIType type, Currency currency) {
            this.type = type;
            this.currency = currency;
        }

        public GUIType getType() {
            return type;
        }

        public Currency getCurrency() {
            return currency;
        }
    }
}