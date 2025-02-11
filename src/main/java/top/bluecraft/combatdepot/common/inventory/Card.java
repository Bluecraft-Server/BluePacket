package top.bluecraft.combatdepot.common.inventory;

import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import top.bluecraft.combatdepot.CombatDepot;
import top.bluecraft.combatdepot.api.ICard;
import top.bluecraft.combatdepot.config.CardConfig;

import java.util.ArrayList;
import java.util.List;

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

    public List<ItemStack> getPageItems(int page) {
        int start = page * slotsPerPage;
        int end = Math.min(start + slotsPerPage, inventory.size());

        List<ItemStack> items = new ArrayList<>();
        for (int i = start; i < end; i++) {
            items.add(inventory.get(i));
        }
        return items;
    }

    public int getTotalPages() {
        return (int) Math.ceil((double) inventory.size() / slotsPerPage);
    }

    // Getters
    public String getName() {
        return name;
    }

    public NonNullList<ItemStack> getInventory() {
        return inventory;
    }

    public void setInventory(NonNullList<ItemStack> inventory) {
        if (inventory == null) {
            CombatDepot.LOGGER.error("Attempted to set null inventory for card: {}", name);
            return;
        }
        this.inventory = inventory;
    }

    public int getSlotsPerPage() {
        return slotsPerPage;
    }

    public ResourceLocation getTexture() {
        return texture;
    }
}