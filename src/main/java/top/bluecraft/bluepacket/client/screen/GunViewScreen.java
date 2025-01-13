package top.bluecraft.bluepacket.client.screen;

import net.minecraft.client.gui.components.Button;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

import com.mojang.blaze3d.systems.RenderSystem;
import org.jetbrains.annotations.NotNull;
import top.bluecraft.bluepacket.BluePacket;
import top.bluecraft.bluepacket.api.ICard;
import top.bluecraft.bluepacket.client.menu.GunViewMenu;
import top.bluecraft.bluepacket.network.CardSelectionMessage;

public class GunViewScreen extends AbstractContainerScreen<GunViewMenu> {
	// 常量定义
	public static final int IMAGE_WIDTH = 400;
	public static final int IMAGE_HEIGHT = 200;
	private static final int CARD_WIDTH = 32;
	private static final int CARD_HEIGHT = 16;
	private static final int CARD_SPACING = 8;
	private static final int CARDS_PER_ROW = 5;
	private static final int PAGE_INFO_Y_OFFSET = 20;
	private static final int INVENTORY_LABEL_X = 220; // 物品栏标签X坐标
	private static final int INVENTORY_LABEL_Y = 98;  // 物品栏标签Y坐标
	private static final int CARD_NAME_X = 14;        // 卡片名称X坐标
	private static final int CARD_NAME_Y = 5;         // 卡片名称Y坐标
	private Button configButton;

	// 资源位置
	private static final ResourceLocation TEXTURE = new ResourceLocation("bluepacket:textures/gui/gun_view.png");

	// 成员变量
	private final Level world;
	private final int x, y, z;
	private final Player entity;
	private final List<ICard> cards;

	public GunViewScreen(GunViewMenu container, Inventory inventory, Component text) {
		super(container, inventory, text);
		this.world = container.world;
		this.x = container.x;
		this.y = container.y;
		this.z = container.z;
		this.entity = container.entity;
		this.imageWidth = IMAGE_WIDTH;
		this.imageHeight = IMAGE_HEIGHT;
		this.cards = container.cards;
	}

	@Override
	public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		this.renderBackground(guiGraphics);
		super.render(guiGraphics, mouseX, mouseY, partialTicks);

		// 渲染卡片列表
		renderCards(guiGraphics, mouseX, mouseY);

		// 渲染当前选中卡片的页面信息
		renderCurrentCardInfo(guiGraphics);

		this.renderTooltip(guiGraphics, mouseX, mouseY);
	}

	private void renderCards(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		int startX = getCardStartX();
		int startY = getCardStartY();

		// 水平渲染所有卡片
		for (int i = 0; i < cards.size(); i++) {
			ICard card = cards.get(i);
			int cardX = startX + i * (CARD_WIDTH + CARD_SPACING);  // 每个卡片向右偏移
            // 保持相同的Y坐标

            // 渲染卡片
			boolean isSelected = i == menu.selectedCardIndex;
			card.render(guiGraphics, minecraft, cardX, startY, CARD_WIDTH, CARD_HEIGHT, isSelected);

			// 渲染卡片名称提示
			if (isMouseOverCard(mouseX, mouseY, cardX, startY)) {
				guiGraphics.renderTooltip(font,
						Component.literal(card.getName()),
						mouseX, mouseY);
			}
		}
	}

	private void renderCurrentCardInfo(GuiGraphics guiGraphics) {
		if (menu.currentCard != null) {
			menu.currentCard.renderPageInfo(guiGraphics, font,
					leftPos + imageWidth / 2 + imageWidth / 4,
					topPos + PAGE_INFO_Y_OFFSET);
		}
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int gx, int gy) {
		RenderSystem.setShaderColor(1, 1, 1, 1);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();

		guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0,
				imageWidth, imageHeight, imageWidth, imageHeight);

		RenderSystem.disableBlend();
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0) { // 左键点击
			// 检查卡片选择
			if (handleCardSelection(mouseX, mouseY)) {
				return true;
			}

			// 检查页面切换
			if (menu.currentCard != null) {
				menu.currentCard.handleMouseClick(mouseX, mouseY,
						leftPos + imageWidth / 2,
						topPos + PAGE_INFO_Y_OFFSET);
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	private boolean handleCardSelection(double mouseX, double mouseY) {
		int startX = getCardStartX();
		int startY = getCardStartY();

		// 检查每个卡片
		for (int i = 0; i < cards.size(); i++) {
			int cardX = startX + i * (CARD_WIDTH + CARD_SPACING);

            if (isMouseOverCard(mouseX, mouseY, cardX, startY)) {
				if (menu.selectedCardIndex != i) {
					menu.selectedCardIndex = i;
					menu.currentCard = cards.get(i);
					menu.currentCard.switchToPage(0);

					BluePacket.PACKET_HANDLER.sendToServer(
							new CardSelectionMessage(i, x, y, z)
					);
					return true;
				}
			}
		}
		return false;
	}

	@Override
	protected void init() {
		super.init();
		// 添加配置按钮
		if (hasConfigPermission()) {
			this.configButton = Button.builder(Component.translatable("gui.bluepacket.config"), (button) -> {
						if (minecraft != null) {
							minecraft.setScreen(new CardConfigScreen(this));
						}
					})
					.pos(leftPos, topPos - 20)  // 设置按钮位置
					.size(40, 20)  // 设置按钮大小
					.build();

			this.addRenderableWidget(configButton);
		}
	}

	private boolean isMouseOverCard(double mouseX, double mouseY, int cardX, int cardY) {
		return mouseX >= cardX && mouseX < cardX + CARD_WIDTH &&
				mouseY >= cardY && mouseY < cardY + CARD_HEIGHT;
	}

	@Override
	public boolean keyPressed(int key, int b, int c) {
		if (key == 256) { // ESC键
			if (minecraft != null && minecraft.player != null) {
				minecraft.player.closeContainer();
			}
			return true;
		}
		return super.keyPressed(key, b, c);
	}

	public void updateCardSelection(int newIndex) {
		if (newIndex < 0 || newIndex >= cards.size()) {
			BluePacket.LOGGER.warn("Attempted to select invalid card index: {}", newIndex);
			return;
		}

		// 更新菜单状态
		menu.selectedCardIndex = newIndex;
		menu.currentCard = cards.get(newIndex);
		menu.currentPageIndex = 0;  // 重置到第一页

		// 重新加载物品
		menu.loadCurrentPage();

		// 播放选择音效（可选）
		if (minecraft != null && minecraft.player != null) {
			minecraft.player.playSound(
					SoundEvents.UI_BUTTON_CLICK.get(),
					1.0F,
					1.0F
			);
		}

		// 强制重新渲染
		if (minecraft != null) {
			minecraft.tell(() -> {
				// 确保在主线程中更新UI
				this.init(minecraft, this.width, this.height);
			});
		}
	}

	// 添加辅助方法来强制刷新UI
	private void refresh() {
		if (minecraft != null) {
			this.init(minecraft, this.width, this.height);
		}
	}

	@Override
	protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
		// 渲染"物品栏"标签
		guiGraphics.drawString(this.font,
				Component.translatable("container.inventory"),
				INVENTORY_LABEL_X,
				INVENTORY_LABEL_Y,
				4210752);
	}

	private boolean hasConfigPermission() {
		if (minecraft == null || minecraft.player == null) return false;
		return minecraft.player.hasPermissions(2);
	}

	private int getCardStartX() {
		return leftPos + 42;
	}

	private int getCardStartY() {
		return topPos - 16;
	}

	// Getter方法
	public Level getWorld() { return world; }
	public int getX() { return x; }
	public int getY() { return y; }
	public int getZ() { return z; }
	public Player getEntity() { return entity; }
}