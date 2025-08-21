package me.white.nbtvoid;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvents;
import java.util.List;

@Environment(EnvType.CLIENT)
public class ClientChestScreen extends GenericContainerScreen {
    private static final int ITEMS_PER_PAGE = 54; // 6 rows * 9 columns like double chest
    private int currentPage = 0;
    private SimpleInventory displayInventory;
    private boolean slotsLocked = false; // false = разблокированы (можно извлекать), true = заблокированы (только копирование)
    
    private ButtonWidget previousButton;
    private ButtonWidget nextButton;
    private ButtonWidget clearButton;
    private ButtonWidget lockButton;

    public ClientChestScreen(PlayerInventory playerInventory) {
        super(new GenericContainerScreenHandler(ScreenHandlerType.GENERIC_9X6, 0, playerInventory, new SimpleInventory(54), 6), playerInventory, Text.translatable("gui.nbtvoid.clientchest.title"));
        this.displayInventory = (SimpleInventory) this.handler.getInventory();
        updateDisplayInventory();
    }

    @Override
    protected void init() {
        super.init();
        
        // Располагаем кнопки над сундуком
        int buttonY = this.y - 30;
        int centerX = this.x + this.backgroundWidth / 2;
        
        // Previous page button (лево)
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
        
        // Next page button (право от центра)
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
        
        // Lock/Unlock button (левее центра)
        this.lockButton = ButtonWidget.builder(
            this.slotsLocked ? Text.literal("🔒") : Text.literal("🔓"), 
            button -> {
                this.slotsLocked = !this.slotsLocked;
                button.setMessage(this.slotsLocked ? Text.literal("🔒") : Text.literal("🔓"));
                updateButtons(); // Обновляем подсказку
                playClickSound();
            }
        )
        .dimensions(centerX - 45, buttonY, 30, 20)
        .build();
        
        // Clear button (справа)
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
            // Обновляем подсказку для кнопки блокировки
            String statusText = this.slotsLocked ? "Locked (Copy mode)" : "Unlocked (Extract mode)";
            // В Minecraft 1.20.4 tooltip устанавливается по-другому, пока оставим просто иконку
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
        String lockStatus = this.slotsLocked ? "COPY MODE" : "EXTRACT MODE";
        int lockStatusWidth = this.textRenderer.getWidth(lockStatus);
        int lockStatusColor = this.slotsLocked ? 0xFF6B6B : 0x51CF66; // Красноватый для заблокированного, зеленоватый для разблокированного
        context.drawText(this.textRenderer, lockStatus, (this.backgroundWidth - lockStatusWidth) / 2, 18, lockStatusColor, false);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Page navigation with arrow keys
        if (keyCode == 262) { // Right arrow
            List<ItemStack> allItems = ClientChestStorage.getAllItems();
            int maxPages = Math.max(1, (allItems.size() + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE);
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
        // Проверяем что слот существует
        if (slot == null) {
            return;
        }
        
        // Слоты сундука (первые 54 слота: 0-53)
        if (slotId >= 0 && slotId < 54) {
            handleChestSlotClick(slot, slotId, button, actionType);
            return;
        }
        
        // Для слотов инвентаря игрока используем прямое взаимодействие с inventory
        if (slotId >= 54 && this.client != null && this.client.player != null) {
            handlePlayerInventoryClickDirect(slot, slotId, button, actionType);
            return;
        }
    }

    private void handleChestSlotClick(Slot slot, int slotId, int button, SlotActionType actionType) {
        if (this.client == null || this.client.player == null) return;
        
        int actualSlotIndex = this.currentPage * ITEMS_PER_PAGE + slotId;
        ItemStack cursorStack = this.handler.getCursorStack();
        ItemStack slotStack = slot.getStack();
        
        if (actionType == SlotActionType.QUICK_MOVE) {
            // Shift+Click - быстрое перемещение
            handleShiftClickChestSlot(slot, actualSlotIndex);
            return;
        }
        
        if (actionType == SlotActionType.PICKUP) {
            if (button == 0) { // Левый клик
                handleLeftClickChestSlot(slot, actualSlotIndex, cursorStack, slotStack);
            } else if (button == 1) { // Правый клик
                handleRightClickChestSlot(slot, actualSlotIndex, cursorStack, slotStack);
            }
        } else if (actionType == SlotActionType.QUICK_CRAFT) {
            // Двойной клик - собрать все подобные предметы
            handleDoubleClickChestSlot(slot, actualSlotIndex, cursorStack);
        }
        
        updateDisplayInventory();
        ClientChestStorage.save();
        playMoveSound();
    }
    
    private void handleShiftClickChestSlot(Slot slot, int actualSlotIndex) {
        ItemStack slotStack = slot.getStack();
        if (slotStack.isEmpty()) return;
        
        if (this.slotsLocked) {
            // В режиме копирования - копируем в инвентарь игрока
            tryMoveToPlayerInventory(slotStack.copy());
        } else {
            // В режиме извлечения - перемещаем в инвентарь игрока
            if (tryMoveToPlayerInventory(slotStack.copy())) {
                ClientChestStorage.removeItemAt(actualSlotIndex);
                slot.setStack(ItemStack.EMPTY);
            }
        }
    }
    
    private void handleLeftClickChestSlot(Slot slot, int actualSlotIndex, ItemStack cursorStack, ItemStack slotStack) {
        if (!cursorStack.isEmpty() && slotStack.isEmpty()) {
            // Положить предмет из курсора в слот
            ClientChestStorage.setItemAt(actualSlotIndex, cursorStack.copy());
            slot.setStack(cursorStack.copy());
            this.handler.setCursorStack(ItemStack.EMPTY);
        } else if (cursorStack.isEmpty() && !slotStack.isEmpty()) {
            // Взять предмет из слота
            if (this.slotsLocked) {
                // Копирование
                this.handler.setCursorStack(slotStack.copy());
            } else {
                // Извлечение
                this.handler.setCursorStack(slotStack.copy());
                ClientChestStorage.removeItemAt(actualSlotIndex);
                slot.setStack(ItemStack.EMPTY);
            }
        } else if (!cursorStack.isEmpty() && !slotStack.isEmpty()) {
            // Поменять местами или объединить
            if (ItemStack.areEqual(cursorStack, slotStack)) {
                // Объединить стаки
                int totalCount = cursorStack.getCount() + slotStack.getCount();
                int maxStack = Math.min(totalCount, slotStack.getMaxCount());
                int remaining = totalCount - maxStack;
                
                ItemStack newStack = slotStack.copy();
                newStack.setCount(maxStack);
                ClientChestStorage.setItemAt(actualSlotIndex, newStack);
                slot.setStack(newStack);
                
                if (remaining > 0) {
                    cursorStack.setCount(remaining);
                } else {
                    this.handler.setCursorStack(ItemStack.EMPTY);
                }
            } else {
                // Поменять местами
                ItemStack temp = cursorStack.copy();
                this.handler.setCursorStack(slotStack.copy());
                ClientChestStorage.setItemAt(actualSlotIndex, temp);
                slot.setStack(temp);
            }
        }
    }
    
    private void handleRightClickChestSlot(Slot slot, int actualSlotIndex, ItemStack cursorStack, ItemStack slotStack) {
        if (!cursorStack.isEmpty() && slotStack.isEmpty()) {
            // Положить один предмет
            ItemStack singleItem = cursorStack.copy();
            singleItem.setCount(1);
            ClientChestStorage.setItemAt(actualSlotIndex, singleItem);
            slot.setStack(singleItem);
            cursorStack.decrement(1);
            if (cursorStack.getCount() <= 0) {
                this.handler.setCursorStack(ItemStack.EMPTY);
            }
        } else if (cursorStack.isEmpty() && !slotStack.isEmpty()) {
            // Взять половину стака
            int half = (slotStack.getCount() + 1) / 2;
            ItemStack halfStack = slotStack.copy();
            halfStack.setCount(half);
            
            if (this.slotsLocked) {
                // Копирование половины
                this.handler.setCursorStack(halfStack);
            } else {
                // Извлечение половины
                this.handler.setCursorStack(halfStack);
                slotStack.decrement(half);
                if (slotStack.getCount() <= 0) {
                    ClientChestStorage.removeItemAt(actualSlotIndex);
                    slot.setStack(ItemStack.EMPTY);
                } else {
                    ClientChestStorage.setItemAt(actualSlotIndex, slotStack);
                }
            }
        }
    }
    
    private void handleDoubleClickChestSlot(Slot slot, int actualSlotIndex, ItemStack cursorStack) {
        if (cursorStack.isEmpty()) return;
        
        // Попытаться собрать все подобные предметы из сундука
        for (int i = 0; i < ClientChestStorage.getTotalSlots() && cursorStack.getCount() < cursorStack.getMaxCount(); i++) {
            ItemStack chestStack = ClientChestStorage.getItemAt(i);
            if (!chestStack.isEmpty() && ItemStack.areEqual(cursorStack, chestStack)) {
                int canTake = Math.min(chestStack.getCount(), cursorStack.getMaxCount() - cursorStack.getCount());
                if (canTake > 0) {
                    cursorStack.increment(canTake);
                    if (!this.slotsLocked) { // Только в режиме извлечения
                        chestStack.decrement(canTake);
                        if (chestStack.getCount() <= 0) {
                            ClientChestStorage.removeItemAt(i);
                        } else {
                            ClientChestStorage.setItemAt(i, chestStack);
                        }
                    }
                }
            }
        }
    }
    
    private boolean tryMoveToPlayerInventory(ItemStack stack) {
        if (this.client == null || this.client.player == null) return false;
        
        // Попытка добавить в инвентарь игрока
        for (int i = 54; i < this.handler.slots.size(); i++) {
            Slot playerSlot = this.handler.slots.get(i);
            ItemStack playerStack = playerSlot.getStack();
            
            if (playerStack.isEmpty()) {
                playerSlot.setStack(stack.copy());
                return true;
            } else if (ItemStack.areEqual(playerStack, stack)) {
                int canAdd = Math.min(stack.getCount(), playerStack.getMaxCount() - playerStack.getCount());
                if (canAdd > 0) {
                    playerStack.increment(canAdd);
                    stack.decrement(canAdd);
                    if (stack.getCount() <= 0) {
                        return true;
                    }
                }
            }
        }
        return stack.getCount() <= 0;
    }

    private void handlePlayerInventoryClickDirect(Slot slot, int slotId, int button, SlotActionType actionType) {
        ItemStack cursorStack = this.handler.getCursorStack();
        ItemStack slotStack = slot.getStack();
        
        if (actionType == SlotActionType.QUICK_MOVE) {
            // Shift+Click - переместить в сундук
            handleShiftClickPlayerSlot(slot, slotStack);
            return;
        }
        
        if (actionType == SlotActionType.PICKUP) {
            if (button == 0) { // Левый клик
                if (cursorStack.isEmpty() && !slotStack.isEmpty()) {
                    // Взять предмет из слота
                    this.handler.setCursorStack(slotStack.copy());
                    slot.setStack(ItemStack.EMPTY);
                } else if (!cursorStack.isEmpty() && slotStack.isEmpty()) {
                    // Положить предмет в слот
                    slot.setStack(cursorStack.copy());
                    this.handler.setCursorStack(ItemStack.EMPTY);
                } else if (!cursorStack.isEmpty() && !slotStack.isEmpty()) {
                    // Поменять предметы местами или объединить
                    if (ItemStack.areEqual(cursorStack, slotStack)) {
                        // Объединить стаки
                        int totalCount = cursorStack.getCount() + slotStack.getCount();
                        int maxStack = Math.min(totalCount, slotStack.getMaxCount());
                        int remaining = totalCount - maxStack;
                        
                        slotStack.setCount(maxStack);
                        if (remaining > 0) {
                            cursorStack.setCount(remaining);
                        } else {
                            this.handler.setCursorStack(ItemStack.EMPTY);
                        }
                    } else {
                        // Поменять местами
                        ItemStack temp = cursorStack.copy();
                        this.handler.setCursorStack(slotStack.copy());
                        slot.setStack(temp);
                    }
                }
            } else if (button == 1) { // Правый клик
                if (!cursorStack.isEmpty() && slotStack.isEmpty()) {
                    // Положить один предмет
                    ItemStack singleItem = cursorStack.copy();
                    singleItem.setCount(1);
                    slot.setStack(singleItem);
                    cursorStack.decrement(1);
                    if (cursorStack.getCount() <= 0) {
                        this.handler.setCursorStack(ItemStack.EMPTY);
                    }
                } else if (cursorStack.isEmpty() && !slotStack.isEmpty()) {
                    // Взять половину
                    int half = (slotStack.getCount() + 1) / 2;
                    ItemStack halfStack = slotStack.copy();
                    halfStack.setCount(half);
                    this.handler.setCursorStack(halfStack);
                    slotStack.decrement(half);
                    if (slotStack.getCount() <= 0) {
                        slot.setStack(ItemStack.EMPTY);
                    }
                }
            }
        } else if (actionType == SlotActionType.QUICK_CRAFT) {
            // Двойной клик - собрать подобные предметы из инвентаря
            handleDoubleClickPlayerSlot(cursorStack);
        }
    }
    
    private void handleShiftClickPlayerSlot(Slot slot, ItemStack slotStack) {
        if (slotStack.isEmpty()) return;
        
        // Попытаться переместить в сундук
        if (tryMoveToChestStorage(slotStack)) {
            slot.setStack(ItemStack.EMPTY);
            updateDisplayInventory();
            ClientChestStorage.save();
            playMoveSound();
        }
    }
    
    private void handleDoubleClickPlayerSlot(ItemStack cursorStack) {
        if (cursorStack.isEmpty()) return;
        
        // Собрать все подобные предметы из инвентаря игрока
        for (int i = 54; i < this.handler.slots.size() && cursorStack.getCount() < cursorStack.getMaxCount(); i++) {
            Slot playerSlot = this.handler.slots.get(i);
            ItemStack playerStack = playerSlot.getStack();
            
            if (!playerStack.isEmpty() && ItemStack.areEqual(cursorStack, playerStack)) {
                int canTake = Math.min(playerStack.getCount(), cursorStack.getMaxCount() - cursorStack.getCount());
                if (canTake > 0) {
                    cursorStack.increment(canTake);
                    playerStack.decrement(canTake);
                    if (playerStack.getCount() <= 0) {
                        playerSlot.setStack(ItemStack.EMPTY);
                    }
                }
            }
        }
    }
    
    private boolean tryMoveToChestStorage(ItemStack stack) {
        ItemStack remaining = stack.copy();
        
        // Попытаться найти свободное место в сундуке
        for (int i = 0; i < ClientChestStorage.getTotalSlots() && !remaining.isEmpty(); i++) {
            ItemStack chestStack = ClientChestStorage.getItemAt(i);
            
            if (chestStack.isEmpty()) {
                // Пустой слот - занимаем
                ClientChestStorage.setItemAt(i, remaining.copy());
                remaining.setCount(0);
                break;
            } else if (ItemStack.areEqual(chestStack, remaining)) {
                // Можем объединить
                int canAdd = Math.min(remaining.getCount(), chestStack.getMaxCount() - chestStack.getCount());
                if (canAdd > 0) {
                    chestStack.increment(canAdd);
                    ClientChestStorage.setItemAt(i, chestStack);
                    remaining.decrement(canAdd);
                }
            }
        }
        
        return remaining.isEmpty();
    }

    private void playClickSound() {
        if (this.client != null && this.client.player != null) {
            this.client.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 1.0F, 1.0F);
        }
    }
    
    private void playMoveSound() {
        if (this.client != null && this.client.player != null) {
            this.client.player.playSound(SoundEvents.ENTITY_ITEM_PICKUP, 0.2F, 1.0F);
        }
    }

    @Override
    public void close() {
        super.close();
        ClientChestStorage.save();
    }
}