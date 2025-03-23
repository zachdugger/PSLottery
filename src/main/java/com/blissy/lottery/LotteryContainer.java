package com.blissy.lottery;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.blissy.lottery.PixelmonLottery.Currency;
import com.pixelmonmod.pixelmon.client.gui.GuiResources;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.network.IContainerFactory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Predicate;

public class LotteryContainer extends Container {

    public static final DeferredRegister<ContainerType<?>> CONTAINERS = DeferredRegister.create(
            ForgeRegistries.CONTAINERS,
            PixelmonLottery.MOD_ID
    );

    public static final RegistryObject<ContainerType<LotteryContainer>> LOTTERY_CONTAINER =
            CONTAINERS.register("lottery_container",
                    () -> new ContainerType<>((IContainerFactory<LotteryContainer>) (windowId, playerInventory, data) ->
                            new LotteryContainer(windowId, playerInventory)
                    )
            );

    public LotteryContainer(int windowId, PlayerInventory playerInventory) {
        super(LOTTERY_CONTAINER.get(), windowId);
    }

    @Override
    public boolean stillValid(PlayerEntity player) {
        return true;
    }

    @OnlyIn(Dist.CLIENT)
    public static class LotteryScreen extends ContainerScreen<LotteryContainer> {
        private static final ResourceLocation TEXTURE = new ResourceLocation(PixelmonLottery.MOD_ID, "textures/gui/lottery_gui.png");

        private TextFieldWidget amountField;
        private Currency selectedCurrency = null;

        // Predicate for validating numeric input
        private static final Predicate<String> NUMERIC_ONLY = input -> input.matches("\\d*");

        public LotteryScreen(LotteryContainer container, PlayerInventory inventory, ITextComponent title) {
            super(container, inventory, title);
            this.imageWidth = 176;
            this.imageHeight = 166;
        }

        @Override
        protected void init() {
            super.init();

            // Initialize text field for amount input
            this.amountField = new TextFieldWidget(
                    this.font,
                    this.leftPos + 70,
                    this.topPos + 90,
                    36,
                    20,
                    new StringTextComponent("")
            );
            this.amountField.setValue("1");
            this.amountField.setVisible(false);
            this.amountField.setFilter(NUMERIC_ONLY);
            this.children.add(this.amountField);

            // Add currency buttons (3 across one row)
            var serverCurrencyButton = addButton(new Button(
                    this.leftPos + 20,
                    this.topPos + 40,
                    40,
                    20,
                    new StringTextComponent("Coins"),
                    button -> selectCurrency(Currency.SERVER_CURRENCY)
            ));

            var tokensButton = addButton(new Button(
                    this.leftPos + 68,
                    this.topPos + 40,
                    40,
                    20,
                    new StringTextComponent("Tokens"),
                    button -> selectCurrency(Currency.TOKENS)
            ));

            var gemsButton = addButton(new Button(
                    this.leftPos + 116,
                    this.topPos + 40,
                    40,
                    20,
                    new StringTextComponent("Gems"),
                    button -> selectCurrency(Currency.GEMS)
            ));

            // Add confirm entry button
            var confirmButton = addButton(new Button(
                    this.leftPos + 70,
                    this.topPos + 115,
                    36,
                    20,
                    new StringTextComponent("Enter"),
                    button -> confirmEntry()
            ) {
                @Override
                public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
                    this.active = selectedCurrency != null && isValidAmount();
                    super.render(matrixStack, mouseX, mouseY, partialTicks);
                }
            });

            // Add exit button at the bottom
            var exitButton = addButton(new Button(
                    this.leftPos + 68,
                    this.topPos + 140,
                    40,
                    20,
                    new StringTextComponent("Exit"),
                    button -> this.onClose()
            ));
        }

        private void selectCurrency(Currency currency) {
            this.selectedCurrency = currency;
            this.amountField.setVisible(true);

            // Focus the text field for immediate input
            this.amountField.setFocus(true);
        }

        private boolean isValidAmount() {
            try {
                int amount = Integer.parseInt(this.amountField.getValue());
                return amount > 0;
            } catch (NumberFormatException e) {
                return false;
            }
        }

        private void confirmEntry() {
            if (selectedCurrency != null && isValidAmount()) {
                int amount = Integer.parseInt(this.amountField.getValue());
                // Send network message to server
                PixelmonLottery.NETWORK_CHANNEL.sendToServer(
                        new PixelmonLottery.LotteryEntryMessage(selectedCurrency, amount)
                );
                // Reset selection
                this.selectedCurrency = null;
                this.amountField.setValue("1");
                this.amountField.setVisible(false);
            }
        }

        @Override
        public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
            this.renderBackground(matrixStack);
            super.render(matrixStack, mouseX, mouseY, partialTicks);

            if (selectedCurrency != null) {
                this.amountField.render(matrixStack, mouseX, mouseY, partialTicks);

                // Render text for amount input
                this.font.draw(
                        matrixStack,
                        "Amount:",
                        this.leftPos + 20,
                        this.topPos + 95,
                        0x404040
                );

                // Render selected currency info
                this.font.draw(
                        matrixStack,
                        "Selected: " + selectedCurrency.getName(),
                        this.leftPos + 20,
                        this.topPos + 70,
                        0x404040
                );

                // Show player's current balance for the selected currency
                var player = this.minecraft.player;
                if (player != null) {
                    int balance = getCurrencyBalance(player, selectedCurrency);
                    this.font.draw(
                            matrixStack,
                            "Balance: " + balance,
                            this.leftPos + 20,
                            this.topPos + 80,
                            0x404040
                    );
                }
            }

            // Draw title
            this.font.draw(
                    matrixStack,
                    this.title,
                    this.leftPos + (this.imageWidth / 2) - (this.font.width(this.title) / 2),
                    this.topPos + 6,
                    0x404040
            );

            // Get and format next drawing time
            var lotteryData = PixelmonLottery.getInstance().getLotteryData();
            String nextDrawingTime = lotteryData.getFormattedNextDrawingTime();

            // Draw lottery time remaining info
            this.font.draw(
                    matrixStack,
                    "Next drawing: " + nextDrawingTime,
                    this.leftPos + 20,
                    this.topPos + 25,
                    0x404040
            );

            // Display player's entries in each lottery
            if (this.minecraft.player != null) {
                var playerId = this.minecraft.player.getUUID();

                int yOffset = this.topPos + 145;
                for (Currency currency : Currency.values()) {
                    var entries = lotteryData.getPlayerEntryAmount(currency, playerId);
                    if (entries > 0) {
                        this.font.draw(
                                matrixStack,
                                currency.getName() + " entries: " + entries,
                                this.leftPos + 10,
                                yOffset + (Currency.values().length - currency.ordinal()) * 10,
                                0x404040
                        );
                    }
                }
            }
        }

        private int getCurrencyBalance(PlayerEntity player, Currency currency) {
            // This would need a client-side implementation or a server query
            // For now, returning a placeholder value or last known balance
            // In a real implementation, you would query the server for this info
            return 1000;
        }

        @Override
        protected void renderBg(MatrixStack matrixStack, float partialTicks, int mouseX, int mouseY) {
            // Use Pixelmon GUI texture or a standard Minecraft one if you don't have a custom texture
            GuiResources.drawItemStorage(matrixStack, this.leftPos, this.topPos, this.imageWidth, this.imageHeight);
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (this.amountField.isFocused()) {
                return this.amountField.keyPressed(keyCode, scanCode, modifiers);
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        @Override
        public boolean charTyped(char typedChar, int modifiers) {
            if (this.amountField.isFocused()) {
                return this.amountField.charTyped(typedChar, modifiers);
            }
            return super.charTyped(typedChar, modifiers);
        }
    }
}