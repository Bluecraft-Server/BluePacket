package top.bluecraft.bluepacket.common.page;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class PaginatedInventory {

    private static final int ITEMS_PER_PAGE = 110;
    private final Inventory inventory;
    private final List<Page> pages;

    public PaginatedInventory(Inventory inventory) {
        this.inventory = inventory;
        this.pages = new ArrayList<>();
        paginateInventory();
    }

    // 分页物品
    private void paginateInventory() {
        int totalItems = inventory.getContainerSize();
        int totalPages = (int) Math.ceil((double) totalItems / ITEMS_PER_PAGE);

        // 创建每个页面
        for (int i = 0; i < totalPages; i++) {
            int startIndex = i * ITEMS_PER_PAGE;
            int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, totalItems);
            List<ItemStack> pageItems = new ArrayList<>();
            for (int j = startIndex; j < endIndex; j++) {
                pageItems.add(inventory.getItem(j));
            }
            pages.add(new Page(pageItems)); // 每个页面的物品列表
        }
    }

    // 获取指定页面
    public Page getPage(int pageIndex) {
        if (pageIndex < 0 || pageIndex >= pages.size()) {
            throw new IndexOutOfBoundsException("Page index out of bounds");
        }
        return pages.get(pageIndex);
    }

    // 获取总页数
    public int getTotalPages() {
        return pages.size();
    }
}

