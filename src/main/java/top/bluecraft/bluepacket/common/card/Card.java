package top.bluecraft.bluepacket.common.card;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import top.bluecraft.bluepacket.api.ICard;
import top.bluecraft.bluepacket.common.page.Page;

import java.util.ArrayList;
import java.util.List;

public abstract class Card implements ICard {

    private static final int ITEMS_PER_PAGE = 110; // 每个页面最多显示的物品数量
    private final Inventory inventory; // 这个卡片的物品
    private final List<Page> pages; // 当前卡片的所有页面

    public Card(Inventory inventory) {
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

    // 切换到指定的页面
    public void switchToPage(int pageIndex) {
        if (pageIndex >= 0 && pageIndex < pages.size()) {
            Page page = pages.get(pageIndex);
            System.out.println("Switched to page: " + pageIndex);
            // 在这里添加页面显示的逻辑
            // 比如渲染页面物品，显示在UI上等
        } else {
            System.out.println("Invalid page index: " + pageIndex);
        }
    }
}

