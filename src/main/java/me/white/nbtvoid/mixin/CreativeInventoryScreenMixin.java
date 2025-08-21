package me.white.nbtvoid.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.white.nbtvoid.Config;
import me.white.nbtvoid.ModdedCreativeTab;
import me.white.nbtvoid.ModdedCreativeTab.Type;
import me.white.nbtvoid.ClientChestScreen;

import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen.CreativeScreenHandler;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;

@Mixin(CreativeInventoryScreen.class)
public abstract class CreativeInventoryScreenMixin {
    @Shadow private TextFieldWidget searchBox;
    @Shadow private static ItemGroup selectedTab;
    @Shadow private float scrollPosition;
    
    private net.minecraft.client.gui.widget.ButtonWidget clientChestButton;

    @Inject(method = "setSelectedTab(Lnet/minecraft/item/ItemGroup;)V", at = @At("TAIL"))
    private void setSelectedTab(ItemGroup group, CallbackInfo info) {
        if (ModdedCreativeTab.getType(group) == Type.VOID) {
            searchBox.setText(Config.getInstance().getDefaultSearchQuery());
            searchBox.setMaxLength(256);
        }
    }

    @Inject(method = "search()V", at = @At("HEAD"), cancellable = true)
    private void search(CallbackInfo info) {
        if (ModdedCreativeTab.getType(selectedTab) == Type.VOID) {
            info.cancel();
            
            ModdedCreativeTab moddedTab = ModdedCreativeTab.moddedTabs.get(selectedTab);

            String query = searchBox.getText();
            CreativeScreenHandler handler = ((CreativeInventoryScreen)(Object)this).getScreenHandler();
            DefaultedList<ItemStack> itemList = handler.itemList;

            ItemStack infoItem = new ItemStack(Items.PAPER, 1);
            infoItem.set(net.minecraft.component.DataComponentTypes.CUSTOM_NAME, 
                         Text.translatable("itemGroup.nbtvoid.infoItem"));
            // Добавляем кастомные данные чтобы предмет не исчез
            NbtCompound customData = new NbtCompound();
            customData.putBoolean("CustomCreativeLock", true);
            infoItem.set(net.minecraft.component.DataComponentTypes.CUSTOM_DATA, 
                         net.minecraft.component.type.NbtComponent.of(customData));

            itemList.clear();
            itemList.add(infoItem);
            new Thread(new ModdedCreativeTab.AsyncSearcher(handler, moddedTab.getSearchProvider(), query)).start();

            scrollPosition = 0.0f;
        }
    }

    @Inject(method = "init()V", at = @At("TAIL"))
    private void init(CallbackInfo info) {
        MinecraftClient instance = MinecraftClient.getInstance();
        if (!instance.player.isCreative()) return;
        
        // Добавляем кнопку Client Chest в левом нижнем углу
        CreativeInventoryScreen screen = (CreativeInventoryScreen)(Object)this;
        
        this.clientChestButton = net.minecraft.client.gui.widget.ButtonWidget.builder(
            Text.literal("Client Chest"),
            button -> openClientChest()
        )
        .dimensions(10, screen.height - 30, 80, 20)
        .build();
        
        screen.addDrawableChild(this.clientChestButton);
        
        if (ModdedCreativeTab.getType(selectedTab) == Type.VOID) {
            searchBox.setText(Config.getInstance().getDefaultSearchQuery());
            searchBox.setMaxLength(256);
        }
    }
    
    private void openClientChest() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            // Создаем новый экран Client Chest с обычным инвентарем
            ClientChestScreen screen = new ClientChestScreen(client.player.getInventory());
            client.setScreen(screen);
        }
    }
}