package top.bluecraft.combatdepot.common.inventory;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import top.bluecraft.combatdepot.CombatDepot;
import top.bluecraft.combatdepot.api.ICard;
import top.bluecraft.combatdepot.config.CardConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Card implements ICard {
    private final String name;
    private final int slotsPerPage;
    private final ResourceLocation texture;
    private NonNullList<ItemStack> inventory;

    public Card(CardConfig.CardEntry entry, NonNullList<ItemStack> inventory) {
        this.name = entry.getName();
        this.inventory = inventory;
        this.slotsPerPage = 110;
        this.texture = entry.getTexture();
    }

    private final Map<Integer, Integer> extractionLimits = new HashMap<>();  // 槽位 -> 限制数量
    private final Map<Integer, Integer> remainingCounts = new HashMap<>();   // 槽位 -> 剩余数量

    public boolean canExtract(int slotIndex) {
        if (!extractionLimits.containsKey(slotIndex)) {
            return true; // 如果没有设置限制，则允许提取
        }
        int remaining = remainingCounts.getOrDefault(slotIndex, extractionLimits.get(slotIndex));
        return remaining > 0;
    }

    public void setExtractionLimit(int slotIndex, int limit) {
        CombatDepot.LOGGER.info("Setting extraction limit for slot {} to {}", slotIndex, limit);
        if (limit <= 0) {
            extractionLimits.remove(slotIndex);
            remainingCounts.remove(slotIndex);
        } else {
            extractionLimits.put(slotIndex, limit);
            remainingCounts.put(slotIndex, limit); // 重置剩余次数
        }
        CombatDepot.LOGGER.info("Current extraction limits: {}", extractionLimits);
    }

    public void decrementRemainingCount(int slotIndex) {
        if (extractionLimits.containsKey(slotIndex)) {
            int remaining = remainingCounts.getOrDefault(slotIndex, extractionLimits.get(slotIndex));
            if (remaining > 0) {
                remainingCounts.put(slotIndex, remaining - 1);
            }
        }
    }

    public int getExtractionLimit(int slotIndex) {
        return extractionLimits.getOrDefault(slotIndex, 0);
    }

    public int getRemainingCount(int slotIndex) {
        return remainingCounts.getOrDefault(slotIndex, 0);
    }

    // 修改 serializeNBT 方法确保数据被正确保存
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();

        // 保存提取限制
        CompoundTag limitsTag = new CompoundTag();
        for (Map.Entry<Integer, Integer> entry : extractionLimits.entrySet()) {
            limitsTag.putInt(String.valueOf(entry.getKey()), entry.getValue());
        }
        tag.put("ExtractionLimits", limitsTag);

        // 保存剩余次数
        CompoundTag remainingTag = new CompoundTag();
        for (Map.Entry<Integer, Integer> entry : remainingCounts.entrySet()) {
            remainingTag.putInt(String.valueOf(entry.getKey()), entry.getValue());
        }
        tag.put("RemainingCounts", remainingTag);

        return tag;
    }

    // 相应的反序列化方法
    public void deserializeNBT(CompoundTag tag) {
        extractionLimits.clear();
        remainingCounts.clear();

        if (tag.contains("ExtractionLimits")) {
            CompoundTag limitsTag = tag.getCompound("ExtractionLimits");
            for (String key : limitsTag.getAllKeys()) {
                try {
                    int slot = Integer.parseInt(key);
                    extractionLimits.put(slot, limitsTag.getInt(key));
                } catch (NumberFormatException ignored) {}
            }
        }

        if (tag.contains("RemainingCounts")) {
            CompoundTag remainingTag = tag.getCompound("RemainingCounts");
            for (String key : remainingTag.getAllKeys()) {
                try {
                    int slot = Integer.parseInt(key);
                    remainingCounts.put(slot, remainingTag.getInt(key));
                } catch (NumberFormatException ignored) {}
            }
        }
    }
    @Override
    public List<ItemStack> getPageItems(int page) {
        int start = page * slotsPerPage;
        int end = Math.min(start + slotsPerPage, inventory.size());

        List<ItemStack> items = new ArrayList<>();
        for (int i = start; i < end; i++) {
            items.add(inventory.get(i));
        }
        return items;
    }

    @Override
    public int getTotalPages() {
        return (int) Math.ceil((double) inventory.size() / slotsPerPage);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public NonNullList<ItemStack> getInventory() {
        return inventory;
    }

    @Override
    public void setInventory(NonNullList<ItemStack> inventory) {
        if (inventory == null) {
            CombatDepot.LOGGER.error("Attempted to set null inventory for card: {}", name);
            return;
        }
        this.inventory = inventory;
    }

    @Override
    public int getSlotsPerPage() {
        return slotsPerPage;
    }

    @Override
    public ResourceLocation getTexture() {
        return texture;
    }


    public void loadExtractionLimits(CompoundTag tag) {
        extractionLimits.clear();
        if (tag.contains("ExtractionLimits")) {
            CompoundTag limitsTag = tag.getCompound("ExtractionLimits");
            for (String key : limitsTag.getAllKeys()) {
                try {
                    int slot = Integer.parseInt(key);
                    int limit = limitsTag.getInt(key);
                    if (limit > 0) {
                        extractionLimits.put(slot, limit);
                        // 加载限制时，如果没有对应的剩余次数，则初始化为限制值
                        if (!remainingCounts.containsKey(slot)) {
                            remainingCounts.put(slot, limit);
                        }
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
    }

    // 剩余次数相关方法
    public Map<Integer, Integer> getRemainingCounts() {
        return new HashMap<>(remainingCounts);
    }


    public void setRemainingCount(int slot, int count) {
        int limit = getExtractionLimit(slot);
        if (limit > 0) {
            if (count <= 0) {
                remainingCounts.remove(slot);
            } else {
                remainingCounts.put(slot, Math.min(count, limit));
            }
        }
    }

    public void resetRemainingCounts() {
        remainingCounts.clear();
        // 重置所有限制槽位的剩余次数为初始限制值
        remainingCounts.putAll(extractionLimits);
    }


    public boolean isSlotLimited(int slot) {
        return getExtractionLimit(slot) > 0;
    }

    public String getSlotStatus(int slot) {
        int limit = getExtractionLimit(slot);
        if (limit <= 0) {
            return "∞"; // 无限制
        }
        int remaining = getRemainingCount(slot);
        return remaining + "/" + limit;
    }
}