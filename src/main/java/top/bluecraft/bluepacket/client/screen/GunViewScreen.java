package top.bluecraft.bluepacket.client.screen;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.GuiGraphics;

import java.util.HashMap;
import java.util.List;

import com.mojang.blaze3d.systems.RenderSystem;
import org.jetbrains.annotations.NotNull;
import top.bluecraft.bluepacket.api.ICard;
import top.bluecraft.bluepacket.client.menu.GunViewMenu;
import top.bluecraft.bluepacket.common.card.Card;
import top.bluecraft.bluepacket.common.page.Page;

public class GunViewScreen extends AbstractContainerScreen<GunViewMenu> {
	private final static HashMap<String, Object> guistate = GunViewMenu.guistate;
	private final Level world;
	private final int x, y, z;
	private final Player entity;
	private final List<ICard> cards;
	private int selectedCardIndex = 0;
	private int currentPageIndex = 0;
	private static final int CARD_WIDTH = 16;
	private static final int CARD_HEIGHT = 32;
	private static final int CARDS_PER_ROW = 5;
	private static final int CARD_SPACING = 4;

	public GunViewScreen(GunViewMenu container, Inventory inventory, Component text, List<ICard> cards) {
		super(container, inventory, text);
		this.world = container.world;
		this.x = container.x;
		this.y = container.y;
		this.z = container.z;
		this.entity = container.entity;
		this.imageWidth = 400;
		this.imageHeight = 200;
		this.cards = cards;
	}

	private static final ResourceLocation texture = new ResourceLocation("bluepacket:textures/screens/gun_view.png");

	@Override
	public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		this.renderBackground(guiGraphics);
		super.render(guiGraphics, mouseX, mouseY, partialTicks);
		this.renderTooltip(guiGraphics, mouseX, mouseY);
		// 获取当前选中的卡片和页面
		ICard currentCard = cards.get(selectedCardIndex);
		Page currentPage = currentCard.getPage(currentPageIndex);

		// 绘制卡片材质
		// 示例：假设卡片材质绘制在特定位置
		int cardX = this.width / 2 - 88;
		int cardY = this.height / 2 - 16;
        if (this.minecraft != null) {
            this.minecraft.getTextureManager().bindForSetup(currentCard.resourceLocation());
        }
        guiGraphics.blit(currentCard.resourceLocation(), cardX, cardY, 0, 0, 16, 32, 16, 32);

		// 绘制高亮效果（如果是选中的卡片）
		if (selectedCardIndex == 0) {
			guiGraphics.fill(x - 2, y - 2, x + CARD_WIDTH + 2, y + CARD_HEIGHT + 2, 0xFFFFFF00);
		}

		// 绘制页面上的物品
		// 示例：假设每个物品占用一个固定的槽位，您需要根据实际情况调整绘制逻辑
		int startX = (this.width - 176) / 2; // 示例起始位置
		int startY = (this.height - 166) / 2 + 30;
		for (int i = 0; i < currentPage.getItems().size(); i++) {
			ItemStack itemStack = currentPage.getItems().get(i);
			// 绘制物品的方法，您需要根据实际情况实现
			// drawItemStack(itemStack, startX + (i % 9) * 18, startY + (i / 9) * 18);
		}

		// 绘制页码信息
		String pageInfo = "Page " + (currentPageIndex + 1) + " / " + currentCard.getTotalPages();
		guiGraphics.drawCenteredString(this.font, pageInfo, this.width / 2, this.height - 40, 0xFFFFFF);
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int gx, int gy) {
		RenderSystem.setShaderColor(1, 1, 1, 1);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		guiGraphics.blit(texture, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);
		RenderSystem.disableBlend();
	}

	@Override
	public boolean keyPressed(int key, int b, int c) {
		if (key == 256) {
            if (this.minecraft != null) {
                if (this.minecraft.player != null) {
                    this.minecraft.player.closeContainer();
                }
            }
            return true;
		}
		return super.keyPressed(key, b, c);
	}

	@Override
	protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0) { // 左键点击
			ICard currentCard = cards.get(selectedCardIndex);
			int startX = (this.width - (CARDS_PER_ROW * (CARD_WIDTH + CARD_SPACING) - CARD_SPACING)) / 2;
			int startY = 50;

			for (int i = 0; i < cards.size(); i++) {
				int x = startX + (i % CARDS_PER_ROW) * (CARD_WIDTH + CARD_SPACING);
				int y = startY + (i / CARDS_PER_ROW) * (CARD_HEIGHT + CARD_SPACING);

				if (mouseX >= x && mouseX <= x + CARD_WIDTH && mouseY >= y && mouseY <= y + CARD_HEIGHT) {
					selectedCardIndex = i;
					currentPageIndex = 1; // 切换卡片时重置为第一页
					return true;
				}
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public void init() {
		super.init();
	}

    public Level getWorld() {
        return world;
    }

    public int getZ() {
        return z;
    }

    public Player getEntity() {
        return entity;
    }
}
