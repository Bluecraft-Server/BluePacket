package top.bluecraft.bluepacket.common.card;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import top.bluecraft.bluepacket.BluePacket;
import top.bluecraft.bluepacket.api.ICard;
import top.bluecraft.bluepacket.api.IPaginated;
import top.bluecraft.bluepacket.client.screen.GunViewScreen;
import top.bluecraft.bluepacket.common.page.Page;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class Card implements ICard, IPaginated {

    /**
     * 一个页面显示的物品数量
     */
    public static final int ITEMS_PER_PAGE = 110;
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
    private final ResourceLocation texturesResourceLocation;
    /**
     * Card的名称
     */
    private final String name;

    public int currentPageIndex = 0;

    public static final int ARROW_SIZE = 16;

    public Card(Inventory inventory, ResourceLocation textures, String name) {
        this.inventory = inventory;
        this.pages = new ArrayList<>();
        this.texturesResourceLocation = textures;
        this.name = name;
        initCard();
    }

    public Card(Inventory inventory, String name) {
        this.inventory = inventory;
        this.pages = new ArrayList<>();
        this.texturesResourceLocation = new ResourceLocation(BluePacket.MODID, "textures/gui/unknown.png");
        this.name = name;
        initCard();
    }

    abstract ResourceLocation backgroundResourceLocation();

    abstract void initCard();

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
            return new Page(List.of(ItemStack.EMPTY));
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
    public Map<String, ResourceLocation> resourceLocation() {
        Map<String, ResourceLocation> map = new HashMap<>();
        map.put("textures", texturesResourceLocation);
        map.put("background", backgroundResourceLocation());
        return map;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public void bindAndBlit(Minecraft minecraft, GuiGraphics gui, int x, int y, int width, int height) {
        if (minecraft != null) {
            minecraft.getTextureManager().bindForSetup(this.backgroundResourceLocation());
            minecraft.getTextureManager().bindForSetup(this.texturesResourceLocation);
        }
        gui.blit(backgroundResourceLocation(), x, y, 0, 0, width, height, width, height);
        gui.blit(texturesResourceLocation, x, y, 0, 0, width, height, width, height);
    }
    @Override
    public void renderFont(GuiGraphics graphics, Font font, int x, int y) {
        // 绘制页码信息
        String pageInfo = "Page " + (currentPageIndex + 1) + " / " + this.getTotalPages();
        graphics.drawCenteredString(font, pageInfo, x, y, 0xFFFFFF);

        // 调整箭头的垂直位置，确保它们显示在页码的上方或下方
        int arrowY = y + 10;  // 将箭头绘制在页码下方10像素的位置

        // 绘制左箭头 "<"
        graphics.drawCenteredString(font, "<", x - 15, arrowY, 0xFFFFFF);  // 左箭头稍微向左偏移

        // 绘制右箭头 ">"
        graphics.drawCenteredString(font, ">", x + 15, arrowY, 0xFFFFFF);  // 右箭头稍微向右偏移
    }

    @Override
    public void mouseClick(GunViewScreen screen, double mouseX, double mouseY) {
        int x = screen.width / 10 * 9;  // GUI 左上角的 X 坐标
        int y = screen.width / 5;       // GUI 左上角的 Y 坐标
        int width = GunViewScreen.IMAGE_WIDTH;  // GUI 的宽度
        int height = GunViewScreen.IMAGE_HEIGHT;  // GUI 的高度

        // 计算箭头的 X 坐标和 Y 坐标
        int leftArrowX = x - 15;  // 左箭头的 X 坐标，稍微向左偏移
        int rightArrowX = x + 15; // 右箭头的 X 坐标，稍微向右偏移
        int arrowY = y + 10;  // 箭头的 Y 坐标，垂直位置稍微向下偏移

        // 判断点击左箭头（<）
        if (isMouseOverArrow(mouseX, mouseY, leftArrowX, arrowY)) {
            if (currentPageIndex > 0) {
                currentPageIndex--; // 切换到前一页
                System.out.println("Switched to page: " + currentPageIndex);
                updateViewSlot(screen);
            }
        }

        // 判断点击右箭头（>）
        if (isMouseOverArrow(mouseX, mouseY, rightArrowX, arrowY)) {
            if (currentPageIndex < getTotalPages() - 1) {
                currentPageIndex++; // 切换到下一页
                System.out.println("Switched to page: " + currentPageIndex);
                updateViewSlot(screen);
            }
        }
    }

    private void updateViewSlot(GunViewScreen screen) {
        // 获取当前页面的物品
        Page currentPage = getPage(currentPageIndex);
        List<ItemStack> items = currentPage.items();

        // 遍历所有物品并更新到槽位
        for (int i = 0; i < Math.min(items.size(), Card.ITEMS_PER_PAGE); i++) {
            ItemStack itemStack = items.get(i);

            // 通过 i 来映射物品到不同的槽
            // 这里假设每个物品对应一个槽位

            // 更新物品槽
            screen.menu.setItem(i, screen.menu.getStateId(), itemStack); // 更新物品槽
        }
    }

    /**
     * 判断鼠标是否点击了箭头区域
     */
    private boolean isMouseOverArrow(double mouseX, double mouseY, int arrowX, int arrowY) {
        // 箭头区域为一个正方形，宽度和高度都为 ARROW_SIZE
        return mouseX >= arrowX && mouseX <= arrowX + ARROW_SIZE && mouseY >= arrowY && mouseY <= arrowY + ARROW_SIZE;
    }
}

