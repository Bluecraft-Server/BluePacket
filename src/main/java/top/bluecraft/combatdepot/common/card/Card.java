package top.bluecraft.combatdepot.common.card;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import top.bluecraft.combatdepot.CombatDepot;
import top.bluecraft.combatdepot.api.ICard;
import top.bluecraft.combatdepot.api.ICardInventory;
import top.bluecraft.combatdepot.common.page.Page;

import java.util.*;
import java.util.function.Consumer;

public abstract class Card implements ICard {
    // 常量定义
    public static final int ITEMS_PER_PAGE = 110;
    private static final int ARROW_HITBOX_SIZE = 20;
    private static final int PAGE_INFO_COLOR = 0xFFFFFF;
    private static final Component ARROW_LEFT = Component.literal("<");
    private static final Component ARROW_RIGHT = Component.literal(">");
    private static final int ARROW_SPACING = 15;

    // 成员变量
    private final ICardInventory inventory;
    private final String name;
    private final String overlayTexture;
    private final List<Page> pages;
    private int currentPageIndex;
    private final Consumer<Integer> onPageChange;

    protected Card(ICardInventory inventory, String name, String overlayTexture, Consumer<Integer> onPageChange) {
        this.inventory = inventory;
        this.name = name;
        this.overlayTexture = overlayTexture;
        this.pages = new ArrayList<>();
        this.currentPageIndex = 0;
        this.onPageChange = onPageChange;
        initializePages();
    }

    private void initializePages() {
        int totalItems = inventory.getSlots();
        int fullPages = totalItems / ITEMS_PER_PAGE;
        int remainingItems = totalItems % ITEMS_PER_PAGE;

        // 创建完整页
        for (int i = 0; i < fullPages; i++) {
            pages.add(createPage(i * ITEMS_PER_PAGE, (i + 1) * ITEMS_PER_PAGE));
        }

        // 创建最后一个不完整页（如果有）
        if (remainingItems > 0) {
            pages.add(createPage(fullPages * ITEMS_PER_PAGE, fullPages * ITEMS_PER_PAGE + remainingItems));
        }
    }

    private Page createPage(int startIndex, int endIndex) {
        List<ItemStack> items = new ArrayList<>(ITEMS_PER_PAGE);
        // 添加实际物品
        for (int i = startIndex; i < endIndex; i++) {
            items.add(inventory.getStackInSlot(i).copy());
        }
        // 用空物品填充到ITEMS_PER_PAGE
        while (items.size() < ITEMS_PER_PAGE) {
            items.add(ItemStack.EMPTY);
        }
        return new Page(items);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getCurrentPageIndex() {
        return currentPageIndex;
    }

    @Override
    public int getTotalPages() {
        return pages.size();
    }

    @Override
    public Page getPage(int pageIndex) {
        if (pageIndex < 0 || pageIndex >= pages.size()) {
            return new Page(Collections.nCopies(ITEMS_PER_PAGE, ItemStack.EMPTY));
        }
        return pages.get(pageIndex);
    }

    @Override
    public void switchToPage(int pageIndex) {
        if (pageIndex >= 0 && pageIndex < pages.size() && pageIndex != currentPageIndex) {
            currentPageIndex = pageIndex;
            onPageChange.accept(pageIndex);
        }
    }

    @Override
    public ICardInventory getInventory() {
        return inventory;
    }

    public String getOverlayTexture() {
        return overlayTexture;
    }
}

