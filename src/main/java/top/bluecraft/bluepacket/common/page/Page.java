package top.bluecraft.bluepacket.common.page;

import net.minecraft.world.item.ItemStack;
import java.util.List;

public class Page {
    private final List<ItemStack> items;

    public Page(List<ItemStack> items) {
        this.items = items;
    }

    public List<ItemStack> getItems() {
        return items;
    }

    // 其他页面相关的操作
}