package top.bluecraft.combatdepot.api;

import net.minecraftforge.items.ItemStackHandler;
import top.bluecraft.combatdepot.common.page.Page;

public interface ICard {
    /**
     * 获取卡片名称
     */
    String getName();

    /**
     * 获取当前页码
     */
    int getCurrentPageIndex();

    /**
     * 获取总页数
     */
    int getTotalPages();

    /**
     * 获取指定页面的物品列表
     */
    Page getPage(int pageIndex);

    /**
     * 切换到指定页面
     */
    void switchToPage(int pageIndex);


    ItemStackHandler getInventory();

    String getOverlayTexture();
}

