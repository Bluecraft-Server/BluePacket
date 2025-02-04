package top.bluecraft.combatdepot.common.data;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;
import top.bluecraft.combatdepot.config.CardConfig;
import top.bluecraft.combatdepot.network.SyncCardsPacket;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GlobalCardStorage extends SavedData {
    private static final String STORAGE_NAME = "global_card_storage";
    private static final int DEFAULT_SLOT_SIZE = 110;

    // 全局共享库存
    private final Map<String, NonNullList<ItemStack>> sharedInventories = new HashMap<>();

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
                NonNullList<ItemStack> inventory = NonNullList.withSize(DEFAULT_SLOT_SIZE, ItemStack.EMPTY);

                // 加载物品数据
                SyncCardsPacket.getList(cardTag, inventory);

                storage.sharedInventories.put(cardName, inventory);
            }
        }
        return storage;
    }

    @Override
    public @NotNull CompoundTag save(CompoundTag tag) {
        CompoundTag inventoriesTag = new CompoundTag();
        sharedInventories.forEach((cardName, inventory) -> {
            CompoundTag cardTag = new CompoundTag();
            ListTag itemsTag = new ListTag();

            // 保存物品数据
            for (int i = 0; i < inventory.size(); i++) {
                ItemStack stack = inventory.get(i);
                if (!stack.isEmpty()) {
                    CompoundTag itemTag = new CompoundTag();
                    itemTag.putInt("Slot", i);
                    stack.save(itemTag);
                    itemsTag.add(itemTag);
                }
            }

            cardTag.put("Items", itemsTag);
            inventoriesTag.put(cardName, cardTag);
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
                            k -> NonNullList.withSize(entry.getInventorySize(), ItemStack.EMPTY)
                    );
                });
        setDirty();
    }

    public NonNullList<ItemStack> getInventory(String cardName) {
        return sharedInventories.getOrDefault(cardName, NonNullList.withSize(DEFAULT_SLOT_SIZE, ItemStack.EMPTY));
    }

    public void updateInventory(String cardName, NonNullList<ItemStack> inventory) {
        NonNullList<ItemStack> oldInventory = sharedInventories.get(cardName);
        if (oldInventory == null || !oldInventory.equals(inventory)) {
            sharedInventories.put(cardName, inventory);
            setDirty();
        }
    }
}