package me.white.nbtvoid;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;

public class ClientChestStorage {
    public static final Path STORAGE_PATH = FabricLoader.getInstance().getGameDir().resolve("client_chest.nbt");
    private static final int SLOTS_PER_PAGE = 54; // 6 rows * 9 slots
    private static final int MAX_PAGES = 10;
    private static final int TOTAL_SLOTS = SLOTS_PER_PAGE * MAX_PAGES;
    
    private static SimpleInventory inventory = new SimpleInventory(TOTAL_SLOTS);
    
    public static Inventory getInventory() {
        return inventory;
    }
    
    public static void load() {
        if (!Files.exists(STORAGE_PATH)) {
            NbtVoid.LOGGER.info("No client chest file found, creating new storage");
            return;
        }
        
        try {
            NbtCompound fileNbt = NbtIo.read(STORAGE_PATH);
            if (fileNbt != null && fileNbt.contains("Items")) {
                NbtList itemsList = fileNbt.getList("Items").orElse(new NbtList());
                
                // Clear existing inventory
                inventory.clear();
                
                for (int i = 0; i < itemsList.size() && i < TOTAL_SLOTS; i++) {
                    NbtElement element = itemsList.get(i);
                    if (element instanceof NbtCompound) {
                        NbtCompound itemNbt = (NbtCompound) element;
                        int slot = itemNbt.getInt("Slot").orElse(-1);
                        
                        if (slot >= 0 && slot < TOTAL_SLOTS) {
                            // Для 1.20.4 используем CODEC для десериализации как в VoidController
                            NbtCompound stackNbt = itemNbt.getCompound("Item").orElse(new NbtCompound());
                            ItemStack stack = ItemStack.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE, stackNbt)
                                .result().orElse(ItemStack.EMPTY);
                            if (!stack.isEmpty()) {
                                inventory.setStack(slot, stack);
                            }
                        }
                    }
                }
                
                NbtVoid.LOGGER.info("Loaded client chest with {} items", itemsList.size());
            }
        } catch (Exception e) {
            NbtVoid.LOGGER.error("Failed to load client chest: ", e);
        }
    }
    
    public static void save() {
        try {
            NbtCompound fileNbt = new NbtCompound();
            NbtList itemsList = new NbtList();
            
            for (int i = 0; i < TOTAL_SLOTS; i++) {
                ItemStack stack = inventory.getStack(i);
                if (!stack.isEmpty()) {
                    NbtCompound itemNbt = new NbtCompound();
                    itemNbt.putInt("Slot", i);
                    
                    // Для 1.20.4 используем CODEC для сериализации как в VoidController
                    NbtCompound stackNbt = (NbtCompound) ItemStack.CODEC
                        .encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, stack)
                        .result().orElse(new NbtCompound());
                    itemNbt.put("Item", stackNbt);
                    
                    itemsList.add(itemNbt);
                }
            }
            
            fileNbt.put("Items", itemsList);
            NbtIo.write(fileNbt, STORAGE_PATH);
            NbtVoid.LOGGER.info("Saved client chest with {} items", itemsList.size());
            
        } catch (Exception e) {
            NbtVoid.LOGGER.error("Failed to save client chest: ", e);
        }
    }
    
    public static void clear() {
        inventory.clear();
        save();
    }
    
    public static void addItem(ItemStack stack) {
        if (stack.isEmpty()) return;
        
        // Find first empty slot
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            if (inventory.getStack(i).isEmpty()) {
                inventory.setStack(i, stack.copy());
                return;
            }
        }
        
        // If no empty slots, try to merge with existing stacks
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            ItemStack existingStack = inventory.getStack(i);
            if (!existingStack.isEmpty() && existingStack.getItem() == stack.getItem() && 
                ItemStack.areEqual(existingStack, stack)) {
                int combinedCount = Math.min(existingStack.getCount() + stack.getCount(), existingStack.getMaxCount());
                int remainder = existingStack.getCount() + stack.getCount() - combinedCount;
                
                existingStack.setCount(combinedCount);
                stack.setCount(remainder);
                
                if (stack.getCount() <= 0) {
                    return; // Successfully merged all items
                }
            }
        }
        
        NbtVoid.LOGGER.warn("Client chest is full, could not add item: {}", stack.getName().getString());
    }
    
    public static List<ItemStack> getAllItems() {
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            ItemStack stack = inventory.getStack(i);
            if (!stack.isEmpty()) {
                items.add(stack);
            }
        }
        return items;
    }
    
    public static int getUsedSlots() {
        int used = 0;
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            if (!inventory.getStack(i).isEmpty()) {
                used++;
            }
        }
        return used;
    }
    
    public static int getTotalSlots() {
        return TOTAL_SLOTS;
    }
    
    public static void setItemAt(int index, ItemStack stack) {
        if (index >= 0 && index < TOTAL_SLOTS) {
            inventory.setStack(index, stack);
        }
    }
    
    public static void removeItemAt(int index) {
        if (index >= 0 && index < TOTAL_SLOTS) {
            inventory.setStack(index, ItemStack.EMPTY);
        }
    }
    
    public static ItemStack getItemAt(int index) {
        if (index >= 0 && index < TOTAL_SLOTS) {
            return inventory.getStack(index);
        }
        return ItemStack.EMPTY;
    }
}