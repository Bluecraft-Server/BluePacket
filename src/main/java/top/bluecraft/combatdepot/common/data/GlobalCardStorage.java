package top.bluecraft.combatdepot.common.data;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;
import top.bluecraft.combatdepot.CombatDepot;
import top.bluecraft.combatdepot.common.inventory.Card;
import top.bluecraft.combatdepot.common.inventory.menu.CombatDepotMenu;
import top.bluecraft.combatdepot.config.CardConfig;

import java.util.HashMap;
import java.util.Map;

public class GlobalCardStorage extends SavedData {
    private final Map<String, Card> cardInventories = new HashMap<>();

    public static GlobalCardStorage load(CompoundTag tag) {
        GlobalCardStorage storage = new GlobalCardStorage();
        storage.cardInventories.clear();

        if (tag.contains("Cards", Tag.TAG_COMPOUND)) {
            CompoundTag cardsData = tag.getCompound("Cards");

            // 遍历所有卡片
            for (String cardName : cardsData.getAllKeys()) {
                CompoundTag cardTag = cardsData.getCompound(cardName);

                // 创建卡片配置
                CardConfig.CardEntry entry = new CardConfig.CardEntry();
                entry.setName(cardName);
                entry.setTexture(CombatDepot.MODID + ":textures/gui/cards/" + cardName + ".png");

                // 读取基本数据
                int size = cardTag.getInt("Size");
                NonNullList<ItemStack> inventory = NonNullList.withSize(size, ItemStack.EMPTY);

                // 创建卡片实例
                Card card = new Card(entry, inventory);

                // 加载物品数据
                ListTag itemsList = cardTag.getList("Items", Tag.TAG_COMPOUND);
                for (int i = 0; i < itemsList.size(); i++) {
                    CompoundTag slotTag = itemsList.getCompound(i);
                    int slot = slotTag.getInt("Slot");
                    if (slot >= 0 && slot < size) {
                        inventory.set(slot, ItemStack.of(slotTag));
                    }
                }

                // 加载提取限制数据
                if (cardTag.contains("ExtractionData")) {
                    card.deserializeNBT(cardTag.getCompound("ExtractionData"));
                }

                storage.cardInventories.put(cardName, card);
            }
        }
        return storage;
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag) {
        CompoundTag cardsData = new CompoundTag();

        // 遍历所有卡片数据
        cardInventories.forEach((cardName, card) -> {
            CompoundTag cardTag = new CompoundTag();
            ListTag itemsList = new ListTag();

            // 保存物品数据
            NonNullList<ItemStack> inventory = card.getInventory();
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
            cardTag.putInt("Size", inventory.size());

            // 保存提取限制数据
            cardTag.put("ExtractionData", card.serializeNBT());

            cardsData.put(cardName, cardTag);
        });

        tag.put("Cards", cardsData);
        return tag;
    }

    public static GlobalCardStorage get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                GlobalCardStorage::load,
                GlobalCardStorage::new,
                "card_storage" // 数据的唯一标识符
        );
    }

    public static void readRemainingCounts(CompoundTag inventoryTag, Card card) {
        if (inventoryTag.contains("RemainingCounts")) {
            CompoundTag remainingTag = inventoryTag.getCompound("RemainingCounts");
            for (String key : remainingTag.getAllKeys()) {
                try {
                    int slot = Integer.parseInt(key);
                    int count = remainingTag.getInt(key);
                    card.setRemainingCount(slot, count);
                } catch (NumberFormatException ignored) {}
            }
        }
    }

    public Card getInventory(String cardName) {
        return cardInventories.computeIfAbsent(cardName, name -> {
            CardConfig.CardEntry entry = new CardConfig.CardEntry();
            entry.setName(name);
            // 设置纹理路径
            entry.setTexture(CombatDepot.MODID + ":textures/gui/cards/" + name + ".png");
            // 设置默认大小
            entry.setInventorySize(CombatDepotMenu.getSlotSize());

            return new Card(entry, NonNullList.withSize(CombatDepotMenu.getSlotSize(), ItemStack.EMPTY));
        });
    }

    public void updateInventory(String cardName, Card card) {
        cardInventories.put(cardName, card);
        setDirty();
    }

    public boolean hasCard(String cardName) {
        return cardInventories.containsKey(cardName);
    }

    public void removeCard(String cardName) {
        cardInventories.remove(cardName);
        setDirty();
    }

    public Map<String, Card> getAllCards() {
        return new HashMap<>(cardInventories);
    }

    public void clearAllData() {
        cardInventories.clear();
        setDirty();
    }

    public void resetRemainingCounts(String cardName) {
        Card card = cardInventories.get(cardName);
        if (card != null) {
            card.resetRemainingCounts();
            setDirty();
        }
    }

    public void resetAllRemainingCounts() {
        cardInventories.values().forEach(Card::resetRemainingCounts);
        setDirty();
    }

    public void decrementRemainingCount(String cardName, int slot) {
        Card card = cardInventories.get(cardName);
        if (card != null) {
            card.decrementRemainingCount(slot);
            setDirty();
        }
    }
}