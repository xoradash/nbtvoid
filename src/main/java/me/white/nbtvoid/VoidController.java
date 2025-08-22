package me.white.nbtvoid;

import java.util.List;

import com.mojang.brigadier.StringReader;
import com.mojang.datafixers.util.Pair;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.command.argument.NbtPathArgumentType;
import net.minecraft.command.argument.NbtPathArgumentType.NbtPath;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtLong;
import net.minecraft.network.packet.Packet;
import net.minecraft.registry.Registries;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.network.packet.s2c.play.AdvancementUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityEquipmentUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.SetTradeOffersS2CPacket;
import net.minecraft.village.TradeOffer;
import net.minecraft.advancement.Advancement;
import net.minecraft.advancement.AdvancementDisplay;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.data.DataTracker.SerializedEntry;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.decoration.DisplayEntity.ItemDisplayEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import me.white.nbtvoid.NbtVoid;

public class VoidController {
    public static final Path PATH = FabricLoader.getInstance().getGameDir().resolve("void.nbt");

    public static List<VoidEntry> nbtVoid = new ArrayList<>();
    public static boolean updating = false;
    // Защита от рекурсии при установке NBT данных
    private static final ThreadLocal<Boolean> SETTING_NBT = ThreadLocal.withInitial(() -> false);

    public static final Runnable UPDATE_RUNNABLE = new Runnable() {
        @Override
        public void run() {
            if (!Config.getInstance().getDoDynamicUpdate()) return;
            // prevent second run from config menu
            if (updating) return;
            updating = true;
            List<VoidEntry> oldVoid = new ArrayList<>(nbtVoid);
            clear();
            for (VoidEntry entry : oldVoid) addEntry(entry);
            SCAN_WORLD_RUNNABLE.run();
            updating = false;
        }
    };

    public static final Runnable UPDATE_MAX_STORED_ITEMS_RUNNABLE = new Runnable() {
        @Override
        public void run() {
            int newMaxStored = Config.getInstance().getMaxStoredItemRows() * 9;
            if (nbtVoid.size() > newMaxStored) nbtVoid = nbtVoid.subList(0, newMaxStored);
        }
    };
    
    public static final Runnable SCAN_WORLD_RUNNABLE = new Runnable() {
        @Override
        public void run() {
            MinecraftClient client = MinecraftClient.getInstance();
            ClientPlayerEntity player = client.player;
            if (player == null) return;
            
            PlayerInventory inventory = player.getInventory();
            
            // Сканируем инвентарь игрока
            for (int i = 0; i < inventory.size(); i++) {
                addItem(inventory.getStack(i));
            }
            
            // Сканируем дополнительные слоты (через прямой доступ)
            // TODO: После изучения 1.21.8 API добавить сканирование брони и оффханда
            
            // Сканируем эндер сундук
            for (int i = 0; i < player.getEnderChestInventory().size(); i++) {
                addItem(player.getEnderChestInventory().getStack(i));
            }

            // Сканируем сущности в мире
            for (Entity entity : client.world.getEntities()) {
                // Предметы на земле
                if (entity instanceof ItemEntity itemEntity) {
                    addItem(itemEntity.getStack());
                }
                // Предметы в рамках
                if (entity instanceof ItemFrameEntity itemFrameEntity) {
                    addItem(itemFrameEntity.getHeldItemStack());
                }
                // Предметы в дисплеях (для новых версий)
                if (entity instanceof ItemDisplayEntity itemDisplayEntity) {
                    addItem(itemDisplayEntity.getStackReference(0).get());
                }
            }
        }
    };

    public static void load() {
        clear();
        if (Files.exists(PATH)) {
            try {
                NbtCompound fileNbt = NbtIo.read(PATH);
                if (fileNbt != null) {
                    NbtList nbt = fileNbt.getList("entries").orElse(new NbtList());
                    for (NbtElement entry : nbt) {
                        NbtCompound entryCompound = (NbtCompound) entry;
                        NbtCompound item = entryCompound.getCompound("item").orElse(new NbtCompound());
                        long time = entryCompound.getLong("time").orElse(System.currentTimeMillis() / 1000);
                        // Для 1.21.3 используем CODEC вместо fromNbt
                        ItemStack stack = ItemStack.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE, item).result().orElse(ItemStack.EMPTY);
                        if (!stack.isEmpty()) {
                            addEntry(new VoidEntry(stack, Instant.ofEpochSecond(time)));
                        }
                    }
                }
            } catch (Exception e) {
                NbtVoid.LOGGER.error("Couldn't load void file: " + e);
            }
        }
        NbtVoid.LOGGER.info("Loaded NBT void");
    }

    public static void save() {
        try {
            NbtCompound nbt = new NbtCompound();
            NbtList entries = new NbtList();
            for (VoidEntry entry : nbtVoid) {
                NbtCompound entryNbt = new NbtCompound();
                entryNbt.put("time", NbtLong.of(entry.getTime().getEpochSecond()));
                // Для 1.21.3 используем CODEC для сериализации
                NbtCompound itemNbt = (NbtCompound) ItemStack.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, entry.getItem()).result().orElse(new NbtCompound());
                entryNbt.put("item", itemNbt);
                entries.add(entryNbt);
            }
            nbt.put("entries", entries);
            NbtIo.write(nbt, PATH);
        } catch (Exception e) {
            NbtVoid.LOGGER.error("Couldn't save void file: ", e);
        }
        NbtVoid.LOGGER.info("Saved NBT void");
    }

    public static boolean itemEquals(ItemStack first, ItemStack second) {
        // Для 1.21.3, сравниваем предметы и компоненты
        return first.getItem().equals(second.getItem()) && first.getComponents().equals(second.getComponents());
    }

    public static void addItems(Iterable<ItemStack> items) {
        if (items == null) return;
        for (ItemStack item : items) {
            addItem(item);
        }
    }

    public static void addItem(ItemStack item) {
        if (item.isEmpty()) return;
        if (!hasCustomComponents(item)) return;

        // Проверяем NBT фильтры (конвертируем components в NBT для проверки)
        NbtCompound itemNbt = getItemNbt(item);
        if (isIgnored(itemNbt)) return;
        
        ItemStack newItem = item.copy();
        
        // Применяем фильтр remove NBT
        NbtCompound filteredNbt = removeRemoved(itemNbt);
        setItemNbt(newItem, filteredNbt);
        
        newItem.setCount(1);

        for (VoidEntry entry : nbtVoid) {
            if (itemEquals(newItem, entry.getItem())) return;
        }

        nbtVoid.add(0, new VoidEntry(newItem));

        if (nbtVoid.size() > Config.getInstance().getMaxStoredItemRows() * 9) {
            nbtVoid = nbtVoid.subList(0, Config.getInstance().getMaxStoredItemRows() * 9);
        }
    }
    
    private static boolean hasCustomComponents(ItemStack stack) {
        // Для 1.21.8 проверяем наличие кастомных компонентов
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
    
    // Хелпер методы для работы с NBT в контексте компонентов
    private static NbtCompound getItemNbt(ItemStack stack) {
        // Извлекаем NBT из CUSTOM_DATA компонента (не трогаем другие компоненты вроде CUSTOM_NAME)
        if (stack.contains(DataComponentTypes.CUSTOM_DATA)) {
            NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
            return customData != null ? customData.getNbt() : new NbtCompound();
        }
        return new NbtCompound();
    }
    
    private static void setItemNbt(ItemStack stack, NbtCompound nbt) {
        // Защита от рекурсии - не вызываем ItemStackMixin снова
        if (SETTING_NBT.get()) return;
        
        try {
            SETTING_NBT.set(true);
            // Устанавливаем NBT через CUSTOM_DATA компонент
            // НЕ трогаем другие компоненты (CUSTOM_NAME, LORE, ENCHANTMENTS и т.д.)
            if (!nbt.isEmpty()) {
                stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
            } else {
                stack.remove(DataComponentTypes.CUSTOM_DATA);
            }
        } finally {
            SETTING_NBT.set(false);
        }
    }

    public static void addEntry(VoidEntry entry) {
        if (entry.getItem().isEmpty()) return;
        if (!hasCustomComponents(entry.getItem())) return;

        // Проверяем NBT фильтры
        NbtCompound itemNbt = getItemNbt(entry.getItem());
        if (isIgnored(itemNbt)) return;
        
        ItemStack newItem = entry.getItem().copy();
        
        // Применяем фильтр remove NBT
        NbtCompound filteredNbt = removeRemoved(itemNbt);
        setItemNbt(newItem, filteredNbt);

        for (VoidEntry voidEntry : nbtVoid) {
            if (itemEquals(newItem, voidEntry.getItem())) return;
        }

        nbtVoid.add(new VoidEntry(newItem, entry.getTime()));

        if (nbtVoid.size() > Config.getInstance().getMaxStoredItemRows() * 9) {
            nbtVoid = nbtVoid.subList(0, Config.getInstance().getMaxStoredItemRows() * 9);
        }
    }

    public static List<VoidEntry> getNbtVoid() {
        return nbtVoid;
    }

    public static void clear() {
        nbtVoid.clear();
    }
    
    // NBT фильтрация методы из оригинала
    private static boolean isIgnored(NbtCompound nbt) {
        if (nbt == null) return true;
        if (nbt.isEmpty()) return true;

        NbtCompound newNbt = nbt.copy();
        for (String ignoreNbt : Config.getInstance().getIgnoreNbt()) {
            try {
                NbtPath path = new NbtPathArgumentType().parse(new StringReader(ignoreNbt));
                path.remove(newNbt);
            } catch (Exception e) {
                NbtVoid.LOGGER.error("Invalid ignore NBT '" + ignoreNbt + "': " + e);
            }
        }
        return newNbt.isEmpty();
    }

    private static NbtCompound removeRemoved(NbtCompound nbt) {
        NbtCompound newNbt = nbt.copy();
        for (String removeNbt : Config.getInstance().getRemoveNbt()) {
            try {
                NbtPath path = new NbtPathArgumentType().parse(new StringReader(removeNbt));
                path.remove(newNbt);
            } catch (Exception e) {
                NbtVoid.LOGGER.error("Invalid remove NBT '" + removeNbt + "': " + e);
            }
        }
        return newNbt;
    }
    

    public static List<ItemStack> itemsFromText(Text text) {
        List<ItemStack> items = new ArrayList<>();

        // Для 1.21.8 API изменилось - пропускаем эту часть
        // TODO: Обновить после изучения нового HoverEvent API

        return items;
    }

    public static void fromPacket(Packet<?> packet) {
        if (!Config.getInstance().getIsEnabled()) return;
        
        // Обрабатываем разные типы пакетов для автоматического сбора предметов
        if (packet instanceof InventoryS2CPacket inventoryPacket) {
            // TODO: Для 1.21.8 нужно обновить API метод
            // Пока пропускаем
        } else if (packet instanceof ScreenHandlerSlotUpdateS2CPacket slotPacket) {
            addItem(slotPacket.getStack());
        } else if (packet instanceof EntityEquipmentUpdateS2CPacket equipmentPacket) {
            for (Pair<EquipmentSlot, ItemStack> pair : equipmentPacket.getEquipmentList()) {
                addItem(pair.getSecond());
            }
        } else if (packet instanceof EntityTrackerUpdateS2CPacket trackerPacket) {
            for (SerializedEntry<?> entry : trackerPacket.trackedValues()) {
                if (entry.value() instanceof ItemStack itemEntry) {
                    addItem(itemEntry);
                }
            }
        } else if (packet instanceof SetTradeOffersS2CPacket tradePacket) {
            for (TradeOffer offer : tradePacket.getOffers()) {
                addItem(offer.getOriginalFirstBuyItem());
                // TODO: Для 1.21.8 нужно обновить метод доступа к TradedItem
                // offer.getSecondBuyItem().ifPresent(item -> addItem(item));
                addItem(offer.getSellItem());
            }
        } else if (packet instanceof GameMessageS2CPacket messagePacket) {
            for (ItemStack item : itemsFromText(messagePacket.content())) {
                addItem(item);
            }
        } else if (packet instanceof AdvancementUpdateS2CPacket advancementPacket) {
            // Для 1.21.8 структура немного изменилась
            try {
                for (var entry : advancementPacket.getAdvancementsToEarn()) {
                    if (entry.value() != null && entry.value().display().isPresent()) {
                        addItem(entry.value().display().get().getIcon());
                    }
                }
            } catch (Exception e) {
                // Если API изменилось, пропускаем
            }
        }
    }
}