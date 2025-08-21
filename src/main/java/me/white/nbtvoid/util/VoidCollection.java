package me.white.nbtvoid.util;

import me.white.nbtvoid.Config;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemStackSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class VoidCollection {
    private List<ItemStack> items = new ArrayList<>();
    private Set<ItemStack> unique = ItemStackSet.create();
    private int maxSize;
    // flag to avoid recursion in ItemStack mixin
    public boolean isLocked = false;

    public VoidCollection() {
        this.maxSize = -1;
    }

    public VoidCollection(int maxSize) {
        this.maxSize = maxSize;
    }

    public ItemStack remove() {
        ItemStack stack = items.remove(0);
        ItemStack ignored = Util.removeNbt(stack, Config.getInstance().getIgnoreNbt());
        unique.remove(ignored);
        assert items.size() == unique.size();
        return stack;
    }

    public boolean add(ItemStack stack) {
        if (stack == null || !hasCustomComponents(stack)) return false;

        isLocked = true;
        stack = stack.copyWithCount(1);
        ItemStack removed = Util.removeNbt(stack, Config.getInstance().getRemoveNbt());
        ItemStack ignored = Util.removeNbt(removed, Config.getInstance().getIgnoreNbt());
        isLocked = false;

        if (!hasCustomComponents(ignored)) return false;
        if (unique.contains(ignored)) return false;

        if (maxSize >= 0) {
            while (items.size() >= maxSize) {
                remove();
            }
        }

        unique.add(ignored);
        return items.add(removed);
    }
    
    private boolean hasCustomComponents(ItemStack stack) {
        // Проверяем наличие основных кастомных компонентов
        return stack.contains(net.minecraft.component.DataComponentTypes.CUSTOM_NAME) ||
               stack.contains(net.minecraft.component.DataComponentTypes.LORE) ||
               stack.contains(net.minecraft.component.DataComponentTypes.ENCHANTMENTS) ||
               stack.contains(net.minecraft.component.DataComponentTypes.CUSTOM_DATA) ||
               stack.contains(net.minecraft.component.DataComponentTypes.UNBREAKABLE) ||
               stack.contains(net.minecraft.component.DataComponentTypes.ATTRIBUTE_MODIFIERS) ||
               stack.contains(net.minecraft.component.DataComponentTypes.CUSTOM_MODEL_DATA) ||
               stack.contains(net.minecraft.component.DataComponentTypes.POTION_CONTENTS) ||
               stack.contains(net.minecraft.component.DataComponentTypes.WRITTEN_BOOK_CONTENT) ||
               stack.contains(net.minecraft.component.DataComponentTypes.WRITABLE_BOOK_CONTENT) ||
               stack.contains(net.minecraft.component.DataComponentTypes.FIREWORK_EXPLOSION) ||
               stack.contains(net.minecraft.component.DataComponentTypes.FIREWORKS) ||
               stack.contains(net.minecraft.component.DataComponentTypes.BANNER_PATTERNS) ||
               stack.contains(net.minecraft.component.DataComponentTypes.BLOCK_ENTITY_DATA) ||
               stack.contains(net.minecraft.component.DataComponentTypes.ENTITY_DATA) ||
               stack.contains(net.minecraft.component.DataComponentTypes.DAMAGE) ||
               stack.contains(net.minecraft.component.DataComponentTypes.MAP_DECORATIONS) ||
               stack.contains(net.minecraft.component.DataComponentTypes.MAP_ID) ||
               stack.contains(net.minecraft.component.DataComponentTypes.BUCKET_ENTITY_DATA) ||
               stack.contains(net.minecraft.component.DataComponentTypes.DYED_COLOR);
    }

    public void clear() {
        items.clear();
        unique.clear();
    }

    public int getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(int maxSize) {
        this.maxSize = maxSize;
        if (maxSize >= 0) {
            while (items.size() > maxSize) {
                remove();
            }
        }
    }

    public List<ItemStack> getItems() {
        return items;
    }
}
