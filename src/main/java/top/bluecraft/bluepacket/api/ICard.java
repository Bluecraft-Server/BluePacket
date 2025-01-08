package top.bluecraft.bluepacket.api;

import net.minecraft.resources.ResourceLocation;
import top.bluecraft.bluepacket.common.card.Card;
import top.bluecraft.bluepacket.common.page.Page;

public interface ICard {
    /**
     * @param pageIndex 页码
     * @return 获取页面
     */
    Page getPage(int pageIndex);

    /**
     * 获取总页数
     * @return 总页数
     */
    int getTotalPages();

    /**
     * 切换页面
     * @param pageIndex 切换的页码
     */
    void switchToPage(int pageIndex);

    /**
     * @return 材质的资源地址
     */
    ResourceLocation resourceLocation();

    /**
     * @return Card的名称
     */
    String name();
}
