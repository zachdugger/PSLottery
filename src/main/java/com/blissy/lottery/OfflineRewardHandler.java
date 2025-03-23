package com.blissy.lottery;

import com.blissy.lottery.PixelmonLottery.Currency;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Mod.EventBusSubscriber(modid = PixelmonLottery.MOD_ID)
public class OfflineRewardHandler {
    private static final Logger LOGGER = LogManager.getLogger();

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getPlayer() instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = (ServerPlayerEntity) event.getPlayer();

            // Process offline rewards asynchronously
            CompletableFuture.runAsync(() -> {
                try {
                    processOfflineRewards(player);
                } catch (Exception e) {
                    LOGGER.error("Error processing offline rewards for player {}", player.getName().getString(), e);
                }
            });
        }
    }

    private static void processOfflineRewards(ServerPlayerEntity player) {
        Path configDir = player.getServer().getDataDirectory().toPath().resolve("config/pixelmonlottery");
        Path offlineRewardsFile = configDir.resolve("offline_rewards.txt");

        if (!Files.exists(offlineRewardsFile)) {
            return;
        }

        try {
            // Read all rewards from file
            List<String> allRewards = Files.readAllLines(offlineRewardsFile);

            // Find rewards for this player
            UUID playerId = player.getUUID();
            List<String> playerRewards = new ArrayList<>();
            List<String> otherRewards = new ArrayList<>();

            for (String line : allRewards) {
                if (line.startsWith(playerId.toString())) {
                    playerRewards.add(line);
                } else {
                    otherRewards.add(line);
                }
            }

            // Process player rewards
            for (String rewardLine : playerRewards) {
                try {
                    String[] parts = rewardLine.split(",");
                    if (parts.length >= 3) {
                        Currency currency = Currency.valueOf(parts[1]);
                        int amount = Integer.parseInt(parts[2]);

                        // Award the prize
                        awardPrize(player, currency, amount);

                        // Get when the prize was won
                        LocalDateTime winTime = null;
                        if (parts.length >= 4) {
                            try {
                                winTime = LocalDateTime.parse(parts[3]);
                            } catch (Exception e) {
                                LOGGER.warn("Could not parse timestamp for reward: {}", rewardLine);
                            }
                        }

                        // Notify player
                        String timeStr = winTime != null ?
                                " on " + winTime.format(DateTimeFormatter.ofPattern("MMMM d")) : "";

                        player.sendMessage(
                                new StringTextComponent(
                                        "§6[Lottery] §rWhile you were away, you won the " + currency.getName() +
                                                " lottery" + timeStr + "! Your prize of " +
                                                CurrencyUtil.formatCurrency(currency, amount, true) +
                                                " has been added to your account."
                                ),
                                player.getUUID()
                        );
                    }
                } catch (Exception e) {
                    LOGGER.error("Error processing reward line: {}", rewardLine, e);
                }
            }

            // Save remaining rewards
            if (otherRewards.isEmpty()) {
                // Delete the file if no more rewards
                Files.deleteIfExists(offlineRewardsFile);
            } else {
                // Write remaining rewards back to file
                Files.write(offlineRewardsFile, otherRewards, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            }

        } catch (IOException e) {
            LOGGER.error("Error reading offline rewards file", e);
        }
    }

    private static void awardPrize(ServerPlayerEntity player, Currency currency, int amount) {
        // Use our CurrencyUtil to award the prize
        CurrencyUtil.addCurrency(player, currency, amount);
        LOGGER.info("Awarded offline prize of {} {} to {}",
                amount, currency.getName(), player.getName().getString());
    }
}