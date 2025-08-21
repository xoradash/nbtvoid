package me.white.nbtvoid.mixin;

import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.ScreenHandler;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin<T extends ScreenHandler> {
    // Пустой миксин - client chest будет реализован через ModdedCreativeTab
}