package me.white.nbtvoid.mixin;

import me.white.nbtvoid.Config;
import me.white.nbtvoid.VoidController;
import net.minecraft.item.ItemStack;
import net.minecraft.component.ComponentType;
import net.minecraft.component.DataComponentTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    
    // Перехватываем установку компонентов
    @Inject(method = "set", at = @At("RETURN"))
    private void onSetComponent(ComponentType<?> componentType, Object value, CallbackInfoReturnable<Object> cir) {
        if (!Config.getInstance().getIsEnabled()) return;
        
        // Не обрабатываем изменения от нашего кода (защита от рекурсии)
        try {
            // Получаем состояние через рефлекшу
            Class<?> voidControllerClass = Class.forName("me.white.nbtvoid.VoidController");
            java.lang.reflect.Field settingNbtField = voidControllerClass.getDeclaredField("SETTING_NBT");
            settingNbtField.setAccessible(true);
            ThreadLocal<Boolean> settingNbt = (ThreadLocal<Boolean>) settingNbtField.get(null);
            if (settingNbt.get()) return; // Пропускаем если мы сами устанавливаем NBT
        } catch (Exception e) {
            // Если не смогли получить состояние, продолжаем
        }
        
        ItemStack stack = (ItemStack)(Object)this;
        
        // Добавляем в void только если это важный компонент
        if (isImportantComponent(componentType)) {
            VoidController.addItem(stack);
        }
    }
    
    // Перехватываем копирование с компонентами
    @Inject(method = "copyWithCount", at = @At("RETURN"))
    private void onCopyWithCount(int count, CallbackInfoReturnable<ItemStack> cir) {
        if (!Config.getInstance().getIsEnabled()) return;
        
        ItemStack originalStack = (ItemStack)(Object)this;
        ItemStack copiedStack = cir.getReturnValue();
        
        // Если исходный стак имеет кастомные компоненты, добавляем копию в void
        if (hasImportantComponents(originalStack)) {
            VoidController.addItem(copiedStack);
        }
    }
    
    private boolean isImportantComponent(ComponentType<?> componentType) {
        // Список важных компонентов которые должны вызвать сохранение в void
        return componentType == DataComponentTypes.CUSTOM_DATA ||
               componentType == DataComponentTypes.CUSTOM_NAME ||
               componentType == DataComponentTypes.LORE ||
               componentType == DataComponentTypes.ENCHANTMENTS ||
               componentType == DataComponentTypes.ATTRIBUTE_MODIFIERS ||
               componentType == DataComponentTypes.CUSTOM_MODEL_DATA ||
               componentType == DataComponentTypes.DAMAGE ||
               componentType == DataComponentTypes.UNBREAKABLE ||
               componentType == DataComponentTypes.POTION_CONTENTS ||
               componentType == DataComponentTypes.WRITTEN_BOOK_CONTENT ||
               componentType == DataComponentTypes.WRITABLE_BOOK_CONTENT ||
               componentType == DataComponentTypes.FIREWORK_EXPLOSION ||
               componentType == DataComponentTypes.FIREWORKS ||
               componentType == DataComponentTypes.BANNER_PATTERNS ||
               componentType == DataComponentTypes.BLOCK_ENTITY_DATA ||
               componentType == DataComponentTypes.ENTITY_DATA ||
               componentType == DataComponentTypes.MAP_DECORATIONS ||
               componentType == DataComponentTypes.MAP_ID ||
               componentType == DataComponentTypes.BUCKET_ENTITY_DATA ||
               componentType == DataComponentTypes.DYED_COLOR;
    }
    
    private boolean hasImportantComponents(ItemStack stack) {
        // Проверяем наличие важных компонентов
        return stack.contains(DataComponentTypes.CUSTOM_DATA) ||
               stack.contains(DataComponentTypes.CUSTOM_NAME) ||
               stack.contains(DataComponentTypes.LORE) ||
               stack.contains(DataComponentTypes.ENCHANTMENTS) ||
               stack.contains(DataComponentTypes.ATTRIBUTE_MODIFIERS) ||
               stack.contains(DataComponentTypes.CUSTOM_MODEL_DATA) ||
               stack.contains(DataComponentTypes.DAMAGE) ||
               stack.contains(DataComponentTypes.UNBREAKABLE) ||
               stack.contains(DataComponentTypes.POTION_CONTENTS) ||
               stack.contains(DataComponentTypes.WRITTEN_BOOK_CONTENT) ||
               stack.contains(DataComponentTypes.WRITABLE_BOOK_CONTENT) ||
               stack.contains(DataComponentTypes.FIREWORK_EXPLOSION) ||
               stack.contains(DataComponentTypes.FIREWORKS) ||
               stack.contains(DataComponentTypes.BANNER_PATTERNS) ||
               stack.contains(DataComponentTypes.BLOCK_ENTITY_DATA) ||
               stack.contains(DataComponentTypes.ENTITY_DATA) ||
               stack.contains(DataComponentTypes.MAP_DECORATIONS) ||
               stack.contains(DataComponentTypes.MAP_ID) ||
               stack.contains(DataComponentTypes.BUCKET_ENTITY_DATA) ||
               stack.contains(DataComponentTypes.DYED_COLOR);
    }
}