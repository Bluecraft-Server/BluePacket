package top.bluecraft.combatdepot.common.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import top.bluecraft.combatdepot.config.CardConfig;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GlobalCardStorage extends SavedData {
    private static final String STORAGE_NAME = "global_card_storage";
    private static final int DEFAULT_SLOT_SIZE = 110;

    // 全局共享库存
    private final Map<String, ItemStackHandler> sharedInventories = new HashMap<>();

    public static GlobalCardStorage get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                GlobalCardStorage::load,
                GlobalCardStorage::new,
                STORAGE_NAME
        );
    }

    private static GlobalCardStorage load(CompoundTag tag) {
        GlobalCardStorage storage = new GlobalCardStorage();
        if (tag.contains("SharedInventories", Tag.TAG_COMPOUND)) {
            CompoundTag inventoriesTag = tag.getCompound("SharedInventories");

            for (String cardName : inventoriesTag.getAllKeys()) {
                CompoundTag cardTag = inventoriesTag.getCompound(cardName);
                ItemStackHandler handler = new ItemStackHandler(DEFAULT_SLOT_SIZE);
                handler.deserializeNBT(cardTag);

                if (handler.getSlots() != DEFAULT_SLOT_SIZE) {
                    ItemStackHandler newHandler = new ItemStackHandler(DEFAULT_SLOT_SIZE);
                    for (int i = 0; i < Math.min(handler.getSlots(), DEFAULT_SLOT_SIZE); i++) {
                        newHandler.setStackInSlot(i, handler.getStackInSlot(i));
                    }
                    handler = newHandler;
                }

                storage.sharedInventories.put(cardName, handler);
            }
        }
        return storage;
    }

    @Override
    public @NotNull CompoundTag save(CompoundTag tag) {
        CompoundTag inventoriesTag = new CompoundTag();
        sharedInventories.forEach((cardName, handler) -> {
            inventoriesTag.put(cardName, handler.serializeNBT());
        });
        tag.put("SharedInventories", inventoriesTag);
        return tag;
    }

    public void initializeCards(List<CardConfig.CardEntry> cardEntries) {
        // 清理无效卡片库存
        sharedInventories.keySet().removeIf(cardName ->
                cardEntries.stream().noneMatch(entry -> entry.getName().equals(cardName) && entry.isEnabled())
        );

        // 初始化缺失的卡片库存
        cardEntries.stream()
                .filter(CardConfig.CardEntry::isEnabled)
                .forEach(entry -> {
                    sharedInventories.computeIfAbsent(entry.getName(),
                            k -> new ItemStackHandler(entry.getInventorySize())
                    );
                });
        setDirty();
    }

    public ItemStackHandler getInventory(String cardName) {
        return sharedInventories.get(cardName);
    }

    public void updateInventory(String cardName, ItemStackHandler handler) {
        ItemStackHandler oldHandler = sharedInventories.get(cardName);
        if (oldHandler == null || !oldHandler.equals(handler)) {
            sharedInventories.put(cardName, handler);
            setDirty();
        }
    }
}