package me.white.nbtvoid;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;

public class ClientChestScreenHandler extends ScreenHandler {
    private final Inventory chestInventory;
    
    public ClientChestScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(54));
    }
    
    public ClientChestScreenHandler(int syncId, PlayerInventory playerInventory, Inventory chestInventory) {
        super(ScreenHandlerType.GENERIC_9X6, syncId);
        
        // Проверяем что инвентарь имеет правильный размер
        checkSize(chestInventory, 54);
        this.chestInventory = chestInventory;
        chestInventory.onOpen(playerInventory.player);
        
        // Добавляем слоты сундука (0-53): 6 рядов по 9 слотов
        for (int row = 0; row < 6; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(chestInventory, col + row * 9, 8 + col * 18, 18 + row * 18));
            }
        }
        
        // Добавляем слоты основного инвентаря игрока (54-80): 3 ряда по 9 слотов
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 140 + row * 18));
            }
        }
        
        // Добавляем слоты хотбара (81-89): 1 ряд из 9 слотов
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 198));
        }
    }
    
    public Inventory getInventory() {
        return this.chestInventory;
    }
    
    @Override
    public boolean canUse(PlayerEntity player) {
        return this.chestInventory.canPlayerUse(player);
    }
    
    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        
        if (slot != null && slot.hasStack()) {
            ItemStack stackInSlot = slot.getStack();
            itemStack = stackInSlot.copy();
            
            if (index < 54) {
                // Из сундука в инвентарь игрока
                if (!this.insertItem(stackInSlot, 54, 90, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Из инвентаря игрока в сундук
                if (!this.insertItem(stackInSlot, 0, 54, false)) {
                    return ItemStack.EMPTY;
                }
            }
            
            if (stackInSlot.isEmpty()) {
                slot.setStack(ItemStack.EMPTY);
            } else {
                slot.markDirty();
            }
        }
        
        return itemStack;
    }
    
    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        this.chestInventory.onClose(player);
    }
}