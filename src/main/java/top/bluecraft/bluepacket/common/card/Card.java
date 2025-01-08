package top.bluecraft.bluepacket.common.card;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import top.bluecraft.bluepacket.api.ICard;
import top.bluecraft.bluepacket.api.IPaginated;
import top.bluecraft.bluepacket.common.page.Page;

import java.util.ArrayList;
import java.util.List;

public class Card implements ICard, IPaginated {

    /**
     * 一个页面显示的物品数量
     */
    private static final int ITEMS_PER_PAGE = 110;
    /**
     * Card的物品储存
     */
    private final Inventory inventory;
    /**
     * Card的Page页面（一个集合）
     */
    private final List<Page> pages;
    /**
     * Card的材质（可自定义，最好是16*32像素）
     */
    private final ResourceLocation resourceLocation;
    /**
     * Card的名称
     */
    private final String name;

    public Card(Inventory inventory, ResourceLocation resourceLocation, String name) {
        this.inventory = inventory;
        this.pages = new ArrayList<>();
        this.resourceLocation = resourceLocation;
        this.name = name;
    }

    /**
     * 分页物品
     * 将 `Inventory` 中的物品分页到多个 `Page` 中，每个页面最多包含 110 个物品。
     * 已启用懒加载
     */
    @Override
    public void paginateInventoryIfNecessary(int pageIndex) {
        int totalItems = inventory.getContainerSize();
        int totalPages = (int) Math.ceil((double) totalItems / ITEMS_PER_PAGE);

        for (int i = pages.size(); i <= pageIndex && i < totalPages; i++) {
            int startIndex = i * ITEMS_PER_PAGE;
            int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, totalItems);
            List<ItemStack> pageItems = new ArrayList<>();
            for (int j = startIndex; j < endIndex; j++) {
                pageItems.add(inventory.getItem(j));
            }
            pages.add(new Page(pageItems));
        }
    }

    /**
     * 获取指定页面
     */
    @Override
    public synchronized Page getPage(int pageIndex) {
        if (pageIndex < 0 || pageIndex >= pages.size()) {
            throw new IndexOutOfBoundsException("Page index out of bounds");
        }
        paginateInventoryIfNecessary(pageIndex);
        return pages.get(pageIndex);
    }

    /**
     * 获取总页数
     * @return 总页数
     */
    @Override
    public int getTotalPages() {
        return pages.size();
    }

    /**
     * 切换到指定的页面
     * @param pageIndex Page的页码
     */
    @Override
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

    @Override
    public ResourceLocation resourceLocation() {
        return resourceLocation;
    }

    @Override
    public String name() {
        return name;
    }

}

