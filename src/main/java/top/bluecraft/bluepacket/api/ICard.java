package top.bluecraft.bluepacket.api;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import top.bluecraft.bluepacket.common.page.Page;

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

    /**
     * 渲染卡片
     */
    void render(GuiGraphics graphics, Minecraft minecraft, int x, int y, int width, int height, boolean isSelected);

    /**
     * 渲染页面信息
     */
    void renderPageInfo(GuiGraphics graphics, Font font, int centerX, int y);

    /**
     * 处理鼠标点击
     */
    void handleMouseClick(double mouseX, double mouseY, int centerX, int y);
}

