package top.bluecraft.combatdepot.common.data;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;
import top.bluecraft.combatdepot.common.inventory.menu.CombatDepotMenu;

import java.util.HashMap;
import java.util.Map;

public class GlobalCardStorage extends SavedData {
    // 存储每个卡片名称对应的物品栏数据
    private final Map<String, NonNullList<ItemStack>> cardInventories = new HashMap<>();

    public static GlobalCardStorage load(CompoundTag tag) {
        GlobalCardStorage storage = new GlobalCardStorage();
        storage.cardInventories.clear();

        if (tag.contains("Cards", Tag.TAG_COMPOUND)) {
            CompoundTag cardsData = tag.getCompound("Cards");

            // 遍历所有卡片
            for (String cardName : cardsData.getAllKeys()) {
                CompoundTag cardTag = cardsData.getCompound(cardName);
                int size = cardTag.getInt("Size");
                NonNullList<ItemStack> inventory = NonNullList.withSize(size, ItemStack.EMPTY);

                // 加载物品
                ListTag itemsList = cardTag.getList("Items", Tag.TAG_COMPOUND);
                for (int i = 0; i < itemsList.size(); i++) {
                    CompoundTag slotTag = itemsList.getCompound(i);
                    int slot = slotTag.getInt("Slot");
                    if (slot >= 0 && slot < size) {
                        inventory.set(slot, ItemStack.of(slotTag));
                    }
                }

                storage.cardInventories.put(cardName, inventory);
            }
        }
        return storage;
    }

    public static GlobalCardStorage get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                GlobalCardStorage::load,
                GlobalCardStorage::new,
                "card_storage" // 数据的唯一标识符
        );
    }

    @Override
    public @NotNull CompoundTag save(CompoundTag tag) {
        CompoundTag cardsData = new CompoundTag();

        // 遍历所有卡片数据
        cardInventories.forEach((cardName, inventory) -> {
            CompoundTag cardTag = new CompoundTag();
            ListTag itemsList = new ListTag();

            // 保存每个槽位的物品
            saveTags(inventory, cardTag, itemsList);
            cardTag.putInt("Size", inventory.size());
            cardsData.put(cardName, cardTag);
        });

        tag.put("Cards", cardsData);
        return tag;
    }

    public static void saveTags(NonNullList<ItemStack> inventory, CompoundTag cardTag, ListTag itemsList) {
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.get(i);
            if (!stack.isEmpty()) {
                CompoundTag slotTag = new CompoundTag();
                slotTag.putInt("Slot", i);
                stack.save(slotTag);
                itemsList.add(slotTag);
            }
        }

        cardTag.put("Items", itemsList);
    }

    public NonNullList<ItemStack> getInventory(String cardName) {
        return cardInventories.computeIfAbsent(cardName,
                k -> NonNullList.withSize(CombatDepotMenu.getSlotSize(), ItemStack.EMPTY));
    }

    public void updateInventory(String cardName, NonNullList<ItemStack> inventory) {
        cardInventories.put(cardName, inventory);
        setDirty();
    }
}