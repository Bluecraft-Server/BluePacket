package top.bluecraft.viewlauncher.client.screen;

import net.minecraft.client.gui.components.Button;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import top.bluecraft.viewlauncher.CombatDepot;
import top.bluecraft.viewlauncher.api.ICard;
import top.bluecraft.viewlauncher.api.ICardInventory;
import top.bluecraft.viewlauncher.client.menu.GunViewMenu;
import top.bluecraft.viewlauncher.common.card.Cards;
import top.bluecraft.viewlauncher.common.card.GeneralCard;
import top.bluecraft.viewlauncher.common.page.Page;
import top.bluecraft.viewlauncher.network.CardSelectionMessage;

@OnlyIn(Dist.CLIENT)
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

	// 资源位置
	private static final ResourceLocation TEXTURE = new ResourceLocation(CombatDepot.MODID, "textures/gui/gun_view.png");

	// 成员变量
	private final Level world;
	private final int x, y, z;
	private final Player entity;
	private final List<ICard> cards;
	private final GunViewMenu container;
	private ICard currentCard;

	public GunViewScreen(GunViewMenu container, Inventory inventory, Component text) {
		super(container, inventory, text);

		this.world = container.world;
		this.x = container.x;
		this.y = container.y;
		this.z = container.z;
		this.currentCard = container.currentCard;
		this.cards = container.cards;
		this.entity = container.entity;
		this.imageWidth = IMAGE_WIDTH;
		this.imageHeight = IMAGE_HEIGHT;
		this.container = container;
		initializeCards();
	}

	private void initializeCards() {
		List<ICardInventory> inventories = Cards.CARD_INVENTORIES;
		for (int i = 0; i < inventories.size(); i++) {
			cards.add(new GeneralCard(
					inventories.get(i),
					"card_" + i,
					new ResourceLocation(CombatDepot.MODID, "textures/gui/card_" + i + ".png"),
					this::onPageChanged
			));
		}
		this.currentCard = !cards.isEmpty() ? cards.get(0) : null;
	}

	private void onPageChanged(int newPageIndex) {
		if (currentCard != null && newPageIndex >= 0 && newPageIndex < currentCard.getTotalPages()) {
			menu.currentPageIndex = newPageIndex;
			loadCurrentPage();
		}
	}

	private void loadCurrentPage() {
		if (currentCard == null) return;

		Page page = currentCard.getPage(menu.currentPageIndex);
		menu.loadCurrentPage(page.items());
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0) { // 左键点击
			// 检查卡片选择
			if (handleCardSelection(mouseX, mouseY)) {
				return true;
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	private boolean handleCardSelection(double mouseX, double mouseY) {
		int startX = getCardStartX();
		int startY = getCardStartY();

		// 检查每个卡片
		for (int i = 0; i < cards.size(); i++) {
			int cardX = startX + i * (CARD_WIDTH);

			if (isMouseOverCard(mouseX, mouseY, cardX, startY)) {
				if (menu.selectedCardIndex != i) {
					selectCard(i);
					return true;
				}
			}
		}
		return false;
	}

	private void selectCard(int index) {
		menu.selectedCardIndex = index;
		currentCard = cards.get(index);
		menu.currentPageIndex = 0;

		CombatDepot.PACKET_HANDLER.sendToServer(
				new CardSelectionMessage(index, menu.x, menu.y, menu.z)
		);

		// 加载新选中卡片的第一页
		loadCurrentPage();
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
			int cardX = startX + i * (CARD_WIDTH );  // 每个卡片向右偏移
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
		if (currentCard != null) {
			currentCard.renderPageInfo(guiGraphics, font,
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
	protected void init() {
		super.init();
		// 添加配置按钮
		if (hasConfigPermission()) {
            // 设置按钮位置
            // 设置按钮大小
            Button configButton = Button.builder(Component.translatable("gui." + CombatDepot.MODID + ".config"), (button) -> {
                        if (minecraft != null) {
                            minecraft.setScreen(new CardConfigScreen(this));
                        }
                    })
                    .pos(leftPos, topPos - 20)  // 设置按钮位置
                    .size(40, 20)  // 设置按钮大小
                    .build();

			this.addRenderableWidget(configButton);
		}

		if (currentCard != null) {
			currentCard.addPageButtons(this, width / 2, height - 40);
			loadCurrentPage(); // 初始加载第一页
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
			CombatDepot.LOGGER.warn("Attempted to select invalid card index: {}", newIndex);
			return;
		}

		selectCard(newIndex);

		// 播放选择音效
		if (minecraft != null && minecraft.player != null) {
			minecraft.player.playSound(
					SoundEvents.UI_BUTTON_CLICK.get(),
					1.0F,
					1.0F
			);
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

	public List<ICard> getCards() {
		return cards;
	}

	public ICard getCurrentCard() {
		return currentCard;
	}
}