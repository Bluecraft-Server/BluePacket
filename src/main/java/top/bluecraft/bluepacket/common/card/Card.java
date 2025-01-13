package top.bluecraft.bluepacket.common.card;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import top.bluecraft.bluepacket.BluePacket;
import top.bluecraft.bluepacket.api.ICard;
import top.bluecraft.bluepacket.api.ICardInventory;
import top.bluecraft.bluepacket.common.page.Page;

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
    private final ResourceLocation overlayTexture;
    private final List<Page> pages;
    private int currentPageIndex;
    private final Consumer<Integer> onPageChange;

    protected Card(ICardInventory inventory, String name, ResourceLocation overlayTexture, Consumer<Integer> onPageChange) {
        this.inventory = inventory;
        this.name = name;
        this.overlayTexture = overlayTexture;
        this.pages = new ArrayList<>();
        this.currentPageIndex = 0;
        this.onPageChange = onPageChange;
        initializePages();
    }

    /**
     * 获取背景材质位置，由子类实现
     */
    protected abstract ResourceLocation getBackgroundTexture();

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
    public void render(GuiGraphics graphics, Minecraft minecraft, int x, int y, int width, int height, boolean isSelected) {
        if (minecraft == null) return;

        try {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();

            // 渲染背景
            graphics.blit(getBackgroundTexture(), x, y, 0, 0, width, height, width, height);
            // 渲染overlay
            graphics.blit(overlayTexture, x, y, 0, 0, width, height, width, height);

            // 如果被选中，渲染高亮效果
            if (isSelected) {
                graphics.fill(x, y, x + width, y + height, 0x80FFFFFF);
            }

            RenderSystem.disableBlend();
        } catch (Exception e) {
            BluePacket.LOGGER.error("Failed to render card: {}", name, e);
        }
    }

    @Override
    public void renderPageInfo(GuiGraphics graphics, Font font, int centerX, int y) {
        // 渲染页码
        Component pageInfo = Component.translatable("gui.bluepacket.page", currentPageIndex + 1, getTotalPages());
        graphics.drawCenteredString(font, pageInfo, centerX, y, PAGE_INFO_COLOR);

        // 渲染卡片名称
        graphics.drawCenteredString(font, Component.translatable("gui.bluepacket." + name), centerX, y - 15, PAGE_INFO_COLOR);

        // 渲染箭头（如果可用）
        if (currentPageIndex > 0) {
            graphics.drawCenteredString(font, ARROW_LEFT, centerX - ARROW_SPACING, y + 15, PAGE_INFO_COLOR);
        }
        if (currentPageIndex < getTotalPages() - 1) {
            graphics.drawCenteredString(font, ARROW_RIGHT, centerX + ARROW_SPACING, y + 15, PAGE_INFO_COLOR);
        }
    }

    @Override
    public void handleMouseClick(double mouseX, double mouseY, int centerX, int y) {
        // 检查左箭头点击
        if (isMouseOverArrow(mouseX, mouseY, centerX - ARROW_SPACING, y) && currentPageIndex > 0) {
            switchToPage(currentPageIndex - 1);
        }
        // 检查右箭头点击
        else if (isMouseOverArrow(mouseX, mouseY, centerX + ARROW_SPACING, y) && currentPageIndex < getTotalPages() - 1) {
            switchToPage(currentPageIndex + 1);
        }
    }

    private boolean isMouseOverArrow(double mouseX, double mouseY, int arrowX, int arrowY) {
        double halfSize = ARROW_HITBOX_SIZE / 2.0;
        return mouseX >= arrowX - halfSize && mouseX <= arrowX + halfSize &&
                mouseY >= arrowY - halfSize && mouseY <= arrowY + halfSize;
    }

    @Override
    public ICardInventory getInventory() {
        return inventory;
    }
}

