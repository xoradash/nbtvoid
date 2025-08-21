package me.white.nbtvoid.util;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.NbtPathArgumentType;
import net.minecraft.item.ItemStack;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.ComponentType;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.List;

public class Util {
    public static ItemStack removeNbt(ItemStack stack, List<String> paths) {
        stack = stack.copy();
        
        for (String path : paths) {
            try {
                // Пытаемся парсить как идентификатор компонента
                Identifier componentId = Identifier.tryParse(path);
                if (componentId != null) {
                    ComponentType<?> componentType = Registries.DATA_COMPONENT_TYPE.get(componentId);
                    if (componentType != null) {
                        stack.remove(componentType);
                        continue;
                    }
                }
                
                // Если не удается найти компонент, пробуем стандартные компоненты
                switch (path) {
                    case "Damage" -> stack.remove(net.minecraft.component.DataComponentTypes.DAMAGE);
                    case "display" -> {
                        stack.remove(net.minecraft.component.DataComponentTypes.CUSTOM_NAME);
                        stack.remove(net.minecraft.component.DataComponentTypes.LORE);
                        stack.remove(net.minecraft.component.DataComponentTypes.DYED_COLOR);
                    }
                    case "Enchantments" -> stack.remove(net.minecraft.component.DataComponentTypes.ENCHANTMENTS);
                    case "CustomModelData" -> stack.remove(net.minecraft.component.DataComponentTypes.CUSTOM_MODEL_DATA);
                    case "BlockEntityTag.id" -> stack.remove(net.minecraft.component.DataComponentTypes.BLOCK_ENTITY_DATA);
                    case "SkullOwner.Id" -> stack.remove(net.minecraft.component.DataComponentTypes.PROFILE);
                }
                
            } catch (Exception ignored) {
                // Игнорируем ошибки парсинга
            }
        }
        
        return stack;
    }
}
