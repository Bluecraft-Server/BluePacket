package top.bluecraft.bluepacket.common.page;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.HashSet;
import java.util.Set;

public class Page {
    private final int pageItemInitial = 110;
    private final Set<ItemStack> pageItems = new HashSet<>(pageItemInitial);
    private final Inventory inventory;
    public Page(Inventory inventory) {
        this.inventory = inventory;
        for(int x = 0; x < pageItemInitial; x++) {
            pageItems.add(inventory.getItem(x));
        }
    }
}
