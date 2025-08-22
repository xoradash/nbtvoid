package me.white.nbtvoid.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;
import net.minecraft.component.DataComponentTypes;
import me.white.nbtvoid.NbtVoid;

@Mixin(CreativeInventoryActionC2SPacket.class)
public class CreativeInventoryActionMixin {

    @ModifyVariable(method = "<init>(ILnet/minecraft/item/ItemStack;)V", at = @At("HEAD"), argsOnly = true)
    private static ItemStack sanitizeStackForNetwork(ItemStack stack) {
        if (stack.isEmpty()) {
            return stack;
        }
        
        try {
            // Create a copy to avoid modifying the original
            ItemStack sanitized = stack.copy();
            
            // Remove enchantments that might cause network serialization issues
            boolean wasModified = false;
            
            if (sanitized.contains(DataComponentTypes.ENCHANTMENTS)) {
                sanitized.remove(DataComponentTypes.ENCHANTMENTS);
                wasModified = true;
            }
            
            if (sanitized.contains(DataComponentTypes.STORED_ENCHANTMENTS)) {
                sanitized.remove(DataComponentTypes.STORED_ENCHANTMENTS);
                wasModified = true;
            }
            
            if (wasModified) {
                NbtVoid.LOGGER.info("Sanitized ItemStack {} before sending to server to prevent network crash", 
                                   sanitized.getItem().toString());
            }
            
            return sanitized;
        } catch (Exception e) {
            // If anything goes wrong, return empty stack to prevent crash
            NbtVoid.LOGGER.error("Failed to sanitize ItemStack for network packet, returning empty stack: ", e);
            return ItemStack.EMPTY;
        }
    }
}