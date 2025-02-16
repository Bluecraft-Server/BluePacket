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
    // 存储每个槽位的取出次数限制
    private final Map<Integer, Integer> extractionLimits = new HashMap<>();
    // 存储每个槽位的剩余取出次数
    private final Map<Integer, Integer> remainingCounts = new HashMap<>();
    private NonNullList<ItemStack> inventory;

    public Card(CardConfig.CardEntry entry, NonNullList<ItemStack> inventory) {
        this.name = entry.getName();
        this.inventory = inventory;
        this.slotsPerPage = 110;
        this.texture = entry.getTexture();
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

    // 取出限制相关方法
    public void setExtractionLimit(int slot, int limit) {
        if (limit <= 0) {
            extractionLimits.remove(slot);
            remainingCounts.remove(slot);
        } else {
            extractionLimits.put(slot, limit);
            // 设置新的限制时，重置剩余次数
            remainingCounts.put(slot, limit);
        }
    }

    public int getExtractionLimit(int slot) {
        return extractionLimits.getOrDefault(slot, -1); // -1 表示无限制
    }

    public CompoundTag saveExtractionLimits() {
        CompoundTag tag = new CompoundTag();
        CompoundTag limitsTag = new CompoundTag();

        for (Map.Entry<Integer, Integer> entry : extractionLimits.entrySet()) {
            limitsTag.putInt(String.valueOf(entry.getKey()), entry.getValue());
        }

        tag.put("ExtractionLimits", limitsTag);
        return tag;
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
                } catch (NumberFormatException ignored) {
                }
            }
        }
    }

    // 剩余次数相关方法
    public Map<Integer, Integer> getRemainingCounts() {
        return new HashMap<>(remainingCounts);
    }

    public int getRemainingCount(int slot) {
        int limit = getExtractionLimit(slot);
        if (limit <= 0) {
            return -1; // 无限制
        }
        return remainingCounts.getOrDefault(slot, limit);
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

    public void decrementRemainingCount(int slot) {
        int currentCount = getRemainingCount(slot);
        if (currentCount > 0) { // 只在有限制且还有剩余次数时减少
            setRemainingCount(slot, currentCount - 1);
        }
    }

    public boolean canExtract(int slot) {
        int limit = getExtractionLimit(slot);
        if (limit <= 0) {
            return true; // 无限制
        }
        int remaining = getRemainingCount(slot);
        return remaining > 0;
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