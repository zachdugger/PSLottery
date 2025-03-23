package com.blissy.lottery.commands;

import com.blissy.lottery.BlissyLottery;
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
import java.util.stream.Collectors;

public class LotteryCommand implements CommandExecutor, TabCompleter {
    private final Lottery plugin;

    public LotteryCommand(Lottery plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            // Open the lottery GUI
            new LotteryGUI(plugin).openMainMenu(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "info":
                showLotteryInfo(player);
                break;

            case "enter":
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "Usage: /lottery enter <currency> <amount>");
                    return true;
                }

                String currencyId = args[1].toLowerCase();
                if (!plugin.getCurrencyManager().hasCurrency(currencyId)) {
                    player.sendMessage(ChatColor.RED + "Unknown currency: " + args[1]);
                    return true;
                }

                Currency currency = plugin.getCurrencyManager().getCurrency(currencyId).get();

                try {
                    long amount = Long.parseLong(args[2]);
                    if (amount <= 0) {
                        player.sendMessage(ChatColor.RED + "Amount must be positive.");
                        return true;
                    }

                    plugin.getLotteryManager().addEntry(currency, player, amount);
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "Invalid amount: " + args[2]);
                }
                break;

            case "help":
                showHelp(player);
                break;

            default:
                player.sendMessage(ChatColor.RED + "Unknown sub-command: " + subCommand);
                showHelp(player);
                break;
        }

        return true;
    }

    private void showLotteryInfo(Player player) {
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

    private void showHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "===== Lottery Commands =====");
        player.sendMessage(ChatColor.GOLD + "/lottery" + ChatColor.WHITE + " - Open the lottery GUI");
        player.sendMessage(ChatColor.GOLD + "/lottery info" + ChatColor.WHITE + " - View current lottery information");
        player.sendMessage(ChatColor.GOLD + "/lottery enter <currency> <amount>" +
                ChatColor.WHITE + " - Enter the lottery with specified currency and amount");
        player.sendMessage(ChatColor.GOLD + "/lottery help" + ChatColor.WHITE + " - Show this help message");
        player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "===========================");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("info", "enter", "help").stream()
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