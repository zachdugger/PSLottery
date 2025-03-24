package com.blissy.lottery.commands;

import com.blissy.lottery.Lottery;
import com.blissy.lottery.currency.Currency;
import com.blissy.lottery.gui.LotteryGUI;
import com.blissy.lottery.utils.TimeUtil;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class LotteryCommand implements CommandExecutor, TabCompleter {
    private final Lottery plugin;

    public LotteryCommand(Lottery plugin) {
        this.plugin = plugin;
        plugin.getLogger().info("LotteryCommand registered");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        plugin.getLogger().info("LotteryCommand executed by " + sender.getName() + " with args: " + Arrays.toString(args));

        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
                return true;
            }

            Player player = (Player) sender;

            // Check permission
            if (!player.hasPermission("pslottery.use")) {
                player.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                plugin.getLogger().info("Player " + player.getName() + " lacks pslottery.use permission");
                return true;
            }

            // Open the lottery GUI
            plugin.getLogger().info("Opening lottery GUI for " + player.getName());
            try {
                new LotteryGUI(plugin).openMainMenu(player);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error opening lottery GUI", e);
                player.sendMessage(ChatColor.RED + "An error occurred while opening the lottery GUI.");
            }
            return true;
        }

        String subCommand = args[0].toLowerCase();
        plugin.getLogger().info("Processing subcommand: " + subCommand);

        switch (subCommand) {
            case "info":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
                    return true;
                }

                Player infoPlayer = (Player) sender;
                if (!infoPlayer.hasPermission("pslottery.use")) {
                    infoPlayer.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                    return true;
                }

                showLotteryInfo(infoPlayer);
                break;

            case "enter":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
                    return true;
                }

                Player enterPlayer = (Player) sender;
                if (!enterPlayer.hasPermission("pslottery.use")) {
                    enterPlayer.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                    return true;
                }

                if (args.length < 3) {
                    enterPlayer.sendMessage(ChatColor.RED + "Usage: /lottery enter <currency> <amount>");
                    return true;
                }

                String currencyId = args[1].toLowerCase();
                if (!plugin.getCurrencyManager().hasCurrency(currencyId)) {
                    enterPlayer.sendMessage(ChatColor.RED + "Unknown currency: " + args[1]);
                    return true;
                }

                Currency currency = plugin.getCurrencyManager().getCurrency(currencyId).get();

                try {
                    long amount = Long.parseLong(args[2]);
                    if (amount <= 0) {
                        enterPlayer.sendMessage(ChatColor.RED + "Amount must be positive.");
                        return true;
                    }

                    plugin.getLotteryManager().addEntry(currency, enterPlayer, amount);
                } catch (NumberFormatException e) {
                    enterPlayer.sendMessage(ChatColor.RED + "Invalid amount: " + args[2]);
                }
                break;

            case "help":
                showHelp(sender);
                break;

            case "reload":
                // Check for admin permission
                if (!sender.hasPermission("pslottery.admin")) {
                    sender.sendMessage(ChatColor.RED + "You don't have permission to reload the plugin.");
                    return true;
                }

                sender.sendMessage(ChatColor.GOLD + "Reloading PSLottery plugin...");
                boolean success = plugin.reload();

                if (success) {
                    sender.sendMessage(ChatColor.GREEN + "PSLottery has been successfully reloaded!");
                } else {
                    sender.sendMessage(ChatColor.RED + "Error reloading PSLottery. Check console for details.");
                }
                break;

            default:
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    player.sendMessage(ChatColor.RED + "Unknown sub-command: " + subCommand);
                    showHelp(player);
                } else {
                    sender.sendMessage(ChatColor.RED + "Unknown sub-command: " + subCommand);
                    showHelp(sender);
                }
                break;
        }

        return true;
    }

    private void showLotteryInfo(Player player) {
        plugin.getLogger().info("Showing lottery info to " + player.getName());
        player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "===== Lottery Information =====");

        // Next drawing time
        String formattedTime = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy 'at' HH:mm")
                .format(plugin.getLotteryManager().getNextDrawingTime());

        String timeRemaining = TimeUtil.formatTimeUntil(plugin.getLotteryManager().getNextDrawingTime());

        player.sendMessage(ChatColor.GOLD + "Next drawing: " + ChatColor.WHITE + formattedTime +
                ChatColor.GRAY + " (" + timeRemaining + " remaining)");

        // Current pools
        player.sendMessage(ChatColor.GOLD + "Current pools:");

        for (Currency currency : plugin.getCurrencyManager().getAllCurrencies()) {
            long poolTotal = plugin.getLotteryManager().getPoolTotal(currency);
            int participants = plugin.getLotteryManager().getParticipantCount(currency);

            player.sendMessage("  " + currency.getColoredName() + ChatColor.WHITE + ": " +
                    currency.formatAmount(poolTotal) +
                    ChatColor.GRAY + " (" + participants + " participants)");
        }

        // Player's entries
        player.sendMessage(ChatColor.GOLD + "Your entries:");

        boolean hasEntries = false;
        for (Currency currency : plugin.getCurrencyManager().getAllCurrencies()) {
            long entries = plugin.getLotteryManager().getPlayerEntries(currency, player.getUniqueId());

            if (entries > 0) {
                hasEntries = true;
                player.sendMessage("  " + currency.getColoredName() + ChatColor.WHITE + ": " +
                        entries + " entries");
            }
        }

        if (!hasEntries) {
            player.sendMessage("  " + ChatColor.GRAY + "You don't have any active lottery entries.");
        }

        player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "===========================");
    }

    private void showHelp(CommandSender sender) {
        plugin.getLogger().info("Showing help to " + sender.getName());
        sender.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "===== Lottery Commands =====");
        sender.sendMessage(ChatColor.GOLD + "/lottery" + ChatColor.WHITE + " - Open the lottery GUI");
        sender.sendMessage(ChatColor.GOLD + "/lottery info" + ChatColor.WHITE + " - View current lottery information");
        sender.sendMessage(ChatColor.GOLD + "/lottery enter <currency> <amount>" +
                ChatColor.WHITE + " - Enter the lottery with specified currency and amount");
        sender.sendMessage(ChatColor.GOLD + "/lottery help" + ChatColor.WHITE + " - Show this help message");

        // Only show admin commands to users with the right permission
        if (sender.hasPermission("pslottery.admin")) {
            sender.sendMessage(ChatColor.GOLD + "/lottery reload" + ChatColor.WHITE + " - Reload the plugin configuration");
        }

        sender.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "===========================");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>(Arrays.asList("info", "enter", "help"));

            // Only add admin commands for players with permission
            if (sender.hasPermission("pslottery.admin")) {
                completions.add("reload");
            }

            return completions.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 2 && args[0].equalsIgnoreCase("enter")) {
            return plugin.getCurrencyManager().getAllCurrencies().stream()
                    .map(Currency::getId)
                    .filter(s -> s.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }
}