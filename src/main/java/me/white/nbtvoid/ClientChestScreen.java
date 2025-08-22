package me.white.nbtvoid;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvents;

@Environment(EnvType.CLIENT)
public class ClientChestScreen extends GenericContainerScreen {
    private static final int ITEMS_PER_PAGE = 54; // 6 rows * 9 columns like double chest
    private int currentPage = 0;
    private SimpleInventory displayInventory;
    
    private ButtonWidget previousButton;
    private ButtonWidget nextButton;
    private ButtonWidget clearButton;
    private ButtonWidget lockButton;

    public ClientChestScreen(PlayerInventory playerInventory) {
        // Используем обычный GenericContainerScreenHandler как у двойного сундука
        super(createGenericHandler(playerInventory), playerInventory, Text.translatable("gui.nbtvoid.clientchest.title"));
        this.displayInventory = (SimpleInventory) this.handler.getInventory();
        updateDisplayInventory();
        
        NbtVoid.LOGGER.info("ClientChest initialized as generic container with {} total slots", this.handler.slots.size());
    }
    
    private static net.minecraft.screen.GenericContainerScreenHandler createGenericHandler(PlayerInventory playerInventory) {
        SimpleInventory chestInventory = new SimpleInventory(54);
        return new net.minecraft.screen.GenericContainerScreenHandler(
            net.minecraft.screen.ScreenHandlerType.GENERIC_9X6, 0, playerInventory, chestInventory, 6
        );
    }

    @Override
    protected void init() {
        super.init();
        
        // Располагаем кнопки над сундуком
        int buttonY = this.y - 30;
        int centerX = this.x + this.backgroundWidth / 2;
        
        // Previous page button
        this.previousButton = ButtonWidget.builder(Text.literal("◀"), button -> {
            if (this.currentPage > 0) {
                this.currentPage--;
                updateDisplayInventory();
                updateButtons();
                playClickSound();
            }
        })
        .dimensions(centerX - 75, buttonY, 25, 20)
        .build();
        
        // Next page button
        this.nextButton = ButtonWidget.builder(Text.literal("▶"), button -> {
            int maxPages = Math.max(1, (ClientChestStorage.getTotalSlots() + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE);
            if (this.currentPage < maxPages - 1) {
                this.currentPage++;
                updateDisplayInventory();
                updateButtons();
                playClickSound();
            }
        })
        .dimensions(centerX + 50, buttonY, 25, 20)
        .build();
        
        // Lock/Unlock button
        this.lockButton = ButtonWidget.builder(
            ClientChestStorage.getSlotsLocked() ? Text.literal("🔒") : Text.literal("🔓"), 
            button -> {
                ClientChestStorage.setSlotsLocked(!ClientChestStorage.getSlotsLocked());
                button.setMessage(ClientChestStorage.getSlotsLocked() ? Text.literal("🔒") : Text.literal("🔓"));
                updateButtons();
                playClickSound();
            }
        )
        .dimensions(centerX - 45, buttonY, 30, 20)
        .build();
        
        // Clear button
        this.clearButton = ButtonWidget.builder(Text.literal("Clear"), button -> {
            ClientChestStorage.clear();
            this.currentPage = 0;
            updateDisplayInventory();
            updateButtons();
            playClickSound();
        })
        .dimensions(centerX + 80, buttonY, 45, 20)
        .build();
        
        this.addDrawableChild(this.previousButton);
        this.addDrawableChild(this.nextButton);
        this.addDrawableChild(this.clearButton);
        this.addDrawableChild(this.lockButton);
        
        updateButtons();
    }
    
    private void updateDisplayInventory() {
        this.displayInventory.clear();
        
        // Отображаем предметы из текущей страницы
        int startIndex = this.currentPage * ITEMS_PER_PAGE;
        for (int i = 0; i < ITEMS_PER_PAGE; i++) {
            int globalIndex = startIndex + i;
            if (globalIndex < ClientChestStorage.getTotalSlots()) {
                ItemStack stack = ClientChestStorage.getItemAt(globalIndex);
                this.displayInventory.setStack(i, stack);
            }
        }
    }
    
    private void updateButtons() {
        int maxPages = Math.max(1, (ClientChestStorage.getTotalSlots() + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE);
        int usedSlots = ClientChestStorage.getUsedSlots();
        
        if (this.previousButton != null) {
            this.previousButton.active = this.currentPage > 0;
        }
        if (this.nextButton != null) {
            this.nextButton.active = this.currentPage < maxPages - 1;
        }
        if (this.clearButton != null) {
            this.clearButton.active = usedSlots > 0;
        }
        if (this.lockButton != null) {
            this.lockButton.setMessage(ClientChestStorage.getSlotsLocked() ? Text.literal("🔒") : Text.literal("🔓"));
        }
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        super.drawForeground(context, mouseX, mouseY);
        
        // Draw page info
        int maxPages = Math.max(1, (ClientChestStorage.getTotalSlots() + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE);
        int usedSlots = ClientChestStorage.getUsedSlots();
        String pageInfo = "Page " + (this.currentPage + 1) + "/" + maxPages + " (" + usedSlots + "/" + ClientChestStorage.getTotalSlots() + " items)";
        int textWidth = this.textRenderer.getWidth(pageInfo);
        context.drawText(this.textRenderer, pageInfo, (this.backgroundWidth - textWidth) / 2, 6, 4210752, false);
        
        // Draw lock status
        String lockStatus = ClientChestStorage.getSlotsLocked() ? "COPY MODE" : "EXTRACT MODE";
        int lockStatusWidth = this.textRenderer.getWidth(lockStatus);
        int lockStatusColor = ClientChestStorage.getSlotsLocked() ? 0xFF6B6B : 0x51CF66;
        context.drawText(this.textRenderer, lockStatus, (this.backgroundWidth - lockStatusWidth) / 2, 18, lockStatusColor, false);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Page navigation with arrow keys
        if (keyCode == 262) { // Right arrow
            int maxPages = Math.max(1, (ClientChestStorage.getTotalSlots() + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE);
            if (this.currentPage < maxPages - 1) {
                this.currentPage++;
                updateDisplayInventory();
                updateButtons();
                return true;
            }
        } else if (keyCode == 263) { // Left arrow  
            if (this.currentPage > 0) {
                this.currentPage--;
                updateDisplayInventory();
                updateButtons();
                return true;
            }
        }
        
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected void onMouseClick(Slot slot, int slotId, int button, SlotActionType actionType) {
        // НЕ вызываем super - работаем напрямую с ScreenHandler
        if (slot == null || this.client == null || this.client.player == null) {
            return;
        }
        
        try {
            // Используем стандартный метод ScreenHandler для обработки кликов
            this.handler.onSlotClick(slotId, button, actionType, this.client.player);
            
            // Синхронизируем изменения с ClientChestStorage только для слотов сундука
            if (slotId >= 0 && slotId < 54) {
                // Отложенное обновление после того как ScreenHandler обработал клик
                this.client.execute(() -> {
                    int actualSlotIndex = this.currentPage * ITEMS_PER_PAGE + slotId;
                    ItemStack currentStack = slot.getStack();
                    
                    if (currentStack.isEmpty()) {
                        ClientChestStorage.removeItemAt(actualSlotIndex);
                    } else {
                        ClientChestStorage.setItemAt(actualSlotIndex, currentStack.copy());
                    }
                    updateDisplayInventory();
                });
            }
        } catch (Exception e) {
            NbtVoid.LOGGER.error("Error handling slot click: ", e);
        }
    }

    private void playClickSound() {
        if (this.client != null && this.client.player != null) {
            this.client.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 1.0F, 1.0F);
        }
    }

    @Override
    public void close() {
        super.close();
        ClientChestStorage.save();
    }
}