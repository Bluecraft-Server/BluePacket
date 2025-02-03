package top.bluecraft.combatdepot.api;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
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


    ICardInventory getInventory();

    String getOverlayTexture();
}

