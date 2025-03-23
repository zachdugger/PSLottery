package com.blissy.lottery;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.blissy.lottery.PixelmonLottery.Currency;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.fml.network.NetworkHooks;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

public class LotteryCommand {

    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(
                Commands.literal("lottery")
                        .executes(LotteryCommand::executeLotteryCommand)
                        .then(Commands.literal("info")
                                .executes(LotteryCommand::executeInfoCommand))
        );
    }

    private static int executeLotteryCommand(CommandContext<CommandSource> context) {
        if (context.getSource().getEntity() instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = (ServerPlayerEntity) context.getSource().getEntity();

            // Show time until next drawing and current entries
            displayNextDrawingInfo(player);
            displayPlayerEntries(player);

            // Open lottery GUI
            NetworkHooks.openGui(
                    player,
                    new LotteryContainerProvider(),
                    buffer -> {}
            );

            return 1;
        } else {
            context.getSource().sendFailure(new StringTextComponent("This command can only be used by players"));
            return 0;
        }
    }

    private static int executeInfoCommand(CommandContext<CommandSource> context) {
        if (context.getSource().getEntity() instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = (ServerPlayerEntity) context.getSource().getEntity();

            // Display comprehensive lottery information
            displayNextDrawingInfo(player);
            displayAllPoolInfo(player);
            displayPlayerEntries(player);

            return 1;
        } else {
            context.getSource().sendFailure(new StringTextComponent("This command can only be used by players"));
            return 0;
        }
    }

    private static void displayNextDrawingInfo(ServerPlayerEntity player) {
        LocalDateTime nextDrawing = PixelmonLottery.getInstance().getLotteryData().getNextDrawingTime();
        LocalDateTime now = LocalDateTime.now();

        long days = ChronoUnit.DAYS.between(now, nextDrawing);
        long hours = ChronoUnit.HOURS.between(now.plusDays(days), nextDrawing);
        long minutes = ChronoUnit.MINUTES.between(now.plusDays(days).plusHours(hours), nextDrawing);

        String timeRemaining = String.format("%d days, %d hours, %d minutes", days, hours, minutes);
        String nextDrawingStr = nextDrawing.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy 'at' HH:mm"));

        player.sendMessage(
                new StringTextComponent("§6[Lottery] §rNext drawing: §e" + nextDrawingStr + " §7(" + timeRemaining + " remaining)"),
                player.getUUID()
        );
    }

    private static void displayAllPoolInfo(ServerPlayerEntity player) {
        PixelmonLottery.LotteryData lotteryData = PixelmonLottery.getInstance().getLotteryData();

        player.sendMessage(
                new StringTextComponent("§6[Lottery] §rCurrent Pool Information:"),
                player.getUUID()
        );

        for (Currency currency : Currency.values()) {
            Map<UUID, Integer> entries = lotteryData.getEntriesForCurrency(currency);
            int totalPool = entries.values().stream().mapToInt(Integer::intValue).sum();
            int participants = entries.size();

            player.sendMessage(
                    new StringTextComponent("§6[Lottery] §r" + currency.getName() + " Pool: " +
                            CurrencyUtil.formatCurrency(currency, totalPool, true) +
                            " §7(" + participants + " participants)"),
                    player.getUUID()
            );
        }
    }

    private static void displayPlayerEntries(ServerPlayerEntity player) {
        PixelmonLottery.LotteryData lotteryData = PixelmonLottery.getInstance().getLotteryData();
        UUID playerId = player.getUUID();
        boolean hasEntries = false;

        for (Currency currency : Currency.values()) {
            int entries = lotteryData.getPlayerEntryAmount(currency, playerId);
            if (entries > 0) {
                if (!hasEntries) {
                    player.sendMessage(
                            new StringTextComponent("§6[Lottery] §rYour current entries:"),
                            playerId
                    );
                    hasEntries = true;
                }

                player.sendMessage(
                        new StringTextComponent("§6[Lottery] §r" + currency.getName() + ": " + entries + " entries"),
                        playerId
                );
            }
        }

        if (!hasEntries) {
            player.sendMessage(
                    new StringTextComponent("§6[Lottery] §rYou don't have any active lottery entries."),
                    playerId
            );
        }
    }

    // Container provider for the lottery GUI
    private static class LotteryContainerProvider implements INamedContainerProvider {
        @Override
        public ITextComponent getDisplayName() {
            return new StringTextComponent("Server Lottery");
        }

        @Override
        public Container createMenu(int windowId, net.minecraft.entity.player.PlayerInventory playerInventory, PlayerEntity player) {
            return new LotteryContainer(windowId, playerInventory);
        }
    }
}