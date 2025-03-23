package com.pixelmon.lottery;

import com.pixelmonmod.pixelmon.Pixelmon;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.network.PacketBuffer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ChatType;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Mod("pixelmonlottery")
public class PixelmonLottery {
    public static final String MOD_ID = "pixelmonlottery";
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String NETWORK_VERSION = "1.0.0";
    public static SimpleChannel NETWORK_CHANNEL;

    private Path configDir;
    private Path lotteryDataFile;
    private LotteryData lotteryData;

    public PixelmonLottery() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void setup(final FMLCommonSetupEvent event) {
        LOGGER.info("Initializing Pixelmon Lottery Plugin");

        setupNetwork();
    }

    public static void setupNetwork() {
        // Register network channel
        NETWORK_CHANNEL = NetworkRegistry.newSimpleChannel(
                new ResourceLocation(MOD_ID, "main"),
                () -> NETWORK_VERSION,
                version -> version.equals(NETWORK_VERSION),
                version -> version.equals(NETWORK_VERSION)
        );

        // Register network messages
        int messageIndex = 0;
        NETWORK_CHANNEL.registerMessage(
                messageIndex++,
                LotteryEntryMessage.class,
                LotteryEntryMessage::encode,
                LotteryEntryMessage::decode,
                LotteryEntryMessage::handle
        );
    }

    @SubscribeEvent
    public void onServerStarting(FMLServerStartingEvent event) {
        // Register commands
        LotteryCommand.register(event.getServer().getCommandManager().getDispatcher());

        // Initialize config directory using Java NIO
        configDir = event.getServer().getDataDirectory().toPath().resolve("config/pixelmonlottery");
        try {
            Files.createDirectories(configDir);
        } catch (IOException e) {
            LOGGER.error("Failed to create config directory", e);
        }

        // Initialize lottery data file
        lotteryDataFile = configDir.resolve("lottery_data.dat");
        loadLotteryData();

        // Schedule weekly lottery drawing task
        MinecraftForge.EVENT_BUS.register(new LotteryScheduler());

        LOGGER.info("Pixelmon Lottery initialized successfully!");
    }

    private void loadLotteryData() {
        if (Files.exists(lotteryDataFile)) {
            try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(lotteryDataFile))) {
                lotteryData = (LotteryData) ois.readObject();
                LOGGER.info("Loaded lottery data: {}", lotteryData);
            } catch (Exception e) {
                LOGGER.error("Failed to load lottery data", e);
                lotteryData = new LotteryData();
            }
        } else {
            lotteryData = new LotteryData();
        }
    }

    public void saveLotteryData() {
        try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(lotteryDataFile))) {
            oos.writeObject(lotteryData);
            LOGGER.info("Saved lottery data: {}", lotteryData);
        } catch (Exception e) {
            LOGGER.error("Failed to save lottery data", e);
        }
    }

    public static PixelmonLottery getInstance() {
        return ModList.get().getModContainerById(MOD_ID)
                .map(ModContainer::getMod)
                .map(PixelmonLottery.class::cast)
                .orElseThrow(() -> new IllegalStateException("PixelmonLottery mod not found"));
    }

    public LotteryData getLotteryData() {
        return lotteryData;
    }

    // Currency enum
    public enum Currency {
        POKECOINS("PokeCoins"),
        TOKENS("Tokens"),
        GEMS("Gems");

        private final String name;

        Currency(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    // Class to store lottery entries
    public static class LotteryData implements Serializable {
        private static final long serialVersionUID = 1L;

        private final Map<Currency, Map<UUID, Integer>> entries = new EnumMap<>(Currency.class);
        private LocalDateTime nextDrawingTime;

        public LotteryData() {
            for (Currency currency : Currency.values()) {
                entries.put(currency, new HashMap<>());
            }

            // Initialize next drawing time to the next Sunday at midnight
            nextDrawingTime = LocalDateTime.now()
                    .with(TemporalAdjusters.next(DayOfWeek.SUNDAY))
                    .withHour(0)
                    .withMinute(0)
                    .withSecond(0);
        }

        public void addEntry(Currency currency, UUID playerId, int amount) {
            Map<UUID, Integer> currencyEntries = entries.get(currency);
            currencyEntries.put(playerId, currencyEntries.getOrDefault(playerId, 0) + amount);
        }

        public Map<UUID, Integer> getEntriesForCurrency(Currency currency) {
            return entries.get(currency);
        }

        public int getPlayerEntryAmount(Currency currency, UUID playerId) {
            return entries.get(currency).getOrDefault(playerId, 0);
        }

        public LocalDateTime getNextDrawingTime() {
            return nextDrawingTime;
        }

        public void setNextDrawingTime(LocalDateTime nextDrawingTime) {
            this.nextDrawingTime = nextDrawingTime;
        }

        public void resetEntriesForCurrency(Currency currency) {
            entries.get(currency).clear();
        }

        public String getFormattedNextDrawingTime() {
            return nextDrawingTime.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy 'at' HH:mm"));
        }

        @Override
        public String toString() {
            return "LotteryData{" +
                    "entries=" + entries.entrySet().stream()
                    .map(e -> e.getKey() + "=" + e.getValue().size() + " entries")
                    .collect(Collectors.joining(", ")) +
                    ", nextDrawingTime=" + nextDrawingTime +
                    '}';
        }
    }

    // Network message for lottery entries
    public static class LotteryEntryMessage {
        private final Currency currency;
        private final int amount;

        public LotteryEntryMessage(Currency currency, int amount) {
            this.currency = currency;
            this.amount = amount;
        }

        public static void encode(LotteryEntryMessage message, PacketBuffer buffer) {
            buffer.writeInt(message.currency.ordinal());
            buffer.writeInt(message.amount);
        }

        public static LotteryEntryMessage decode(PacketBuffer buffer) {
            Currency currency = Currency.values()[buffer.readInt()];
            int amount = buffer.readInt();
            return new LotteryEntryMessage(currency, amount);
        }

        public static void handle(LotteryEntryMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                ServerPlayerEntity player = context.getSender();
                if (player != null) {
                    // Validate that the player has the required currency
                    if (hasCurrency(player, message.currency, message.amount)) {
                        // Deduct currency
                        deductCurrency(player, message.currency, message.amount);

                        // Add lottery entry
                        PixelmonLottery.getInstance().getLotteryData().addEntry(message.currency, player.getUUID(), message.amount);
                        PixelmonLottery.getInstance().saveLotteryData();

                        // Send confirmation message
                        player.sendMessage(new StringTextComponent(
                                "You have entered the " + message.currency.getName() + " lottery with " + message.amount + " " + message.currency.getName()
                        ), player.getUUID());
                    } else {
                        // Send error message for insufficient funds
                        player.sendMessage(new StringTextComponent(
                                "You don't have enough " + message.currency.getName() + " for this entry."
                        ), player.getUUID());
                    }
                }
            });
            context.setPacketHandled(true);
        }

        // Check if player has the required currency
        private static boolean hasCurrency(ServerPlayerEntity player, Currency currency, int amount) {
            // This implementation depends on how your server manages currencies
            // Here's a placeholder implementation
            return switch (currency) {
                case POKECOINS -> Pixelmon.moneyManager.getBankAccount(player.getUUID()).getMoney() >= amount;
                case TOKENS -> true; // Implement token check based on your server's token system
                case GEMS -> true; // Implement gem check based on your server's gem system
            };
        }

        // Deduct currency from player
        private static void deductCurrency(ServerPlayerEntity player, Currency currency, int amount) {
            // This implementation depends on how your server manages currencies
            // Here's a placeholder implementation
            switch (currency) {
                case POKECOINS -> Pixelmon.moneyManager.getBankAccount(player.getUUID()).take(amount);
                case TOKENS -> {}; // Implement token deduction based on your server's token system
                case GEMS -> {}; // Implement gem deduction based on your server's gem system
            }
        }
    }

    // Lottery scheduler class
    public class LotteryScheduler {
        private int tickCounter = 0;

        @SubscribeEvent
        public void onServerTick(TickEvent.ServerTickEvent event) {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }

            // Check every minute (20 ticks/second * 60 seconds)
            if (++tickCounter >= 1200) {
                tickCounter = 0;

                LocalDateTime now = LocalDateTime.now();
                if (now.isAfter(lotteryData.getNextDrawingTime())) {
                    drawLottery();
                }
            }
        }

        private void drawLottery() {
            LOGGER.info("Drawing weekly lottery...");

            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server == null) {
                LOGGER.error("Server is null, cannot draw lottery");
                return;
            }

            ServerWorld world = server.getLevel(World.OVERWORLD);
            if (world == null) {
                LOGGER.error("Overworld is null, cannot draw lottery");
                return;
            }

            // Draw winners for each currency
            for (Currency currency : Currency.values()) {
                Map<UUID, Integer> entries = lotteryData.getEntriesForCurrency(currency);
                if (entries.isEmpty()) {
                    LOGGER.info("No entries for {} lottery, skipping", currency.getName());
                    continue;
                }

                // Calculate total entries and pick a winner
                List<UUID> weightedEntries = new ArrayList<>();
                for (Map.Entry<UUID, Integer> entry : entries.entrySet()) {
                    for (int i = 0; i < entry.getValue(); i++) {
                        weightedEntries.add(entry.getKey());
                    }
                }

                if (!weightedEntries.isEmpty()) {
                    // Select random winner
                    UUID winnerId = weightedEntries.get(new Random().nextInt(weightedEntries.size()));
                    int totalPool = entries.values().stream().mapToInt(Integer::intValue).sum();

                    // Broadcast winner
                    ServerPlayerEntity winner = world.getServer().getPlayerList().getPlayer(winnerId);
                    String winnerName = winner != null ? winner.getName().getString() : "Offline Player";

                    world.getServer().getPlayerList().broadcastMessage(
                            new StringTextComponent(
                                    "§6[Lottery] §r" + winnerName + " has won the " + currency.getName() +
                                            " lottery! Prize: " + totalPool + " " + currency.getName() + "!"
                            ),
                            ChatType.SYSTEM,
                            UUID.randomUUID()
                    );

                    // Award prize to winner
                    if (winner != null) {
                        awardPrize(winner, currency, totalPool);
                    } else {
                        // Store prize for offline player
                        storePrizeForOfflinePlayer(winnerId, currency, totalPool);
                    }
                }

                // Reset entries for this currency
                lotteryData.resetEntriesForCurrency(currency);
            }

            // Update next drawing time
            LocalDateTime nextDrawingTime = LocalDateTime.now()
                    .with(TemporalAdjusters.next(DayOfWeek.SUNDAY))
                    .withHour(0)
                    .withMinute(0)
                    .withSecond(0);

            lotteryData.setNextDrawingTime(nextDrawingTime);

            // Save lottery data
            saveLotteryData();

            LOGGER.info("Lottery drawing completed. Next drawing scheduled for: {}",
                    lotteryData.getFormattedNextDrawingTime());
        }

        private void awardPrize(ServerPlayerEntity player, Currency currency, int amount) {
            // This implementation depends on how your server manages currencies
            // Here's a placeholder implementation
            switch (currency) {
                case POKECOINS -> Pixelmon.moneyManager.getBankAccount(player.getUUID()).add(amount);
                case TOKENS -> {}; // Implement token award based on your server's token system
                case GEMS -> {}; // Implement gem award based on your server's gem system
            }

            player.sendMessage(new StringTextComponent(
                    "§6[Lottery] §rCongratulations! You won the " + currency.getName() +
                            " lottery! Your prize of " + amount + " " + currency.getName() +
                            " has been added to your account."
            ), player.getUUID());
        }

        private void storePrizeForOfflinePlayer(UUID playerId, Currency currency, int amount) {
            // Store prize for when player logs in
            // Implementation depends on your server's persistence solution

            // For this example, we'll create a simple offline rewards file
            try {
                Path offlineRewardsFile = configDir.resolve("offline_rewards.txt");
                String reward = playerId + "," + currency.name() + "," + amount + "," + LocalDateTime.now() + System.lineSeparator();

                if (Files.exists(offlineRewardsFile)) {
                    Files.writeString(offlineRewardsFile, reward, java.nio.file.StandardOpenOption.APPEND);
                } else {
                    Files.writeString(offlineRewardsFile, reward);
                }

                LOGGER.info("Stored offline reward for player {}: {} {}", playerId, amount, currency.getName());
            } catch (IOException e) {
                LOGGER.error("Failed to store offline reward", e);
            }
        }
    }
}