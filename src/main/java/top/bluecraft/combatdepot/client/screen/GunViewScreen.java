package top.bluecraft.combatdepot.client.screen;

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
import top.bluecraft.combatdepot.CombatDepot;
import top.bluecraft.combatdepot.api.ICard;
import top.bluecraft.combatdepot.api.ICardInventory;
import top.bluecraft.combatdepot.client.Colors;
import top.bluecraft.combatdepot.common.inventory.menu.GunViewMenu;
import top.bluecraft.combatdepot.common.card.Cards;
import top.bluecraft.combatdepot.common.card.GeneralCard;
import top.bluecraft.combatdepot.common.page.Page;
import top.bluecraft.combatdepot.network.CardSelectionMessage;
import top.bluecraft.combatdepot.network.LoadPageMessage;

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

	private static final int VISIBLE_CARDS = 8; // 一次显示的卡片数量
	private static final int ARROW_WIDTH = 12;
	private static final int ARROW_HEIGHT = 12;
	private static final int ARROW_PADDING = 4;

	// 资源位置
	private static final ResourceLocation TEXTURE = new ResourceLocation(CombatDepot.MODID, "textures/gui/gun_view.png");
	private static final ResourceLocation ARROWS_TEXTURE = new ResourceLocation(CombatDepot.MODID, "textures/gui/arrows.png");

	// 成员变量
	private final Level world;
	private final int x, y, z;
	private final Player entity;
	private final List<ICard> cards = new ArrayList<>();
	private final GunViewMenu container;
	private ICard currentCard;
	private int currentPageIndex = 0;


	private int currentCardOffset = 0;
	private Button leftArrowButton;
	private Button rightArrowButton;

	public GunViewScreen(GunViewMenu container, Inventory inventory, Component text) {
		super(container, inventory, text);

		this.world = container.world;
		this.x = container.x;
		this.y = container.y;
		this.z = container.z;
		this.currentCard = !cards.isEmpty() ? cards.get(0) : null;

		// 加载第一页
		if (currentCard != null) {
			loadCurrentPage();
		}
		this.entity = container.entity;
		this.imageWidth = IMAGE_WIDTH;
		this.imageHeight = IMAGE_HEIGHT;
		this.container = container;
		initializeCards();
	}

	private void initializeCards() {
		List<ICardInventory> inventories = Cards.CARD_INVENTORIES;
		for (ICardInventory inventory : inventories) {
			cards.add(new GeneralCard(
					inventory,
					inventory.getName(), // 使用库存的名称作为卡片名称
					new ResourceLocation(CombatDepot.MODID, "textures/gui/cards/" + inventory.getName() + ".png"),
					this::onPageChanged
			));
		}
		this.currentCard = !cards.isEmpty() ? cards.get(0) : null;
	}

	private void onPageChanged(int newPageIndex) {
		if (currentCard != null && newPageIndex >= 0 && newPageIndex < currentCard.getTotalPages()) {
			currentPageIndex = newPageIndex;
			loadCurrentPage();
		}
	}

	private void loadCurrentPage() {
		if (currentCard == null) return;

		Page page = currentCard.getPage(currentPageIndex);
		if (page != null) {
			CombatDepot.PACKET_HANDLER.sendToServer(new LoadPageMessage(currentPageIndex, page.items()));
		}
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

		// 检查可见卡片范围内的点击
		for (int i = 0; i < Math.min(VISIBLE_CARDS, cards.size() - currentCardOffset); i++) {
			int cardIndex = i + currentCardOffset;
			int cardX = startX + i * (CARD_WIDTH + CARD_SPACING);

			if (isMouseOverCard(mouseX, mouseY, cardX, startY)) {
				if (menu.selectedCardIndex != cardIndex) {
					selectCard(cardIndex);
					return true;
				}
			}
		}
		return false;
	}

	private void selectCard(int index) {
		if (index >= 0 && index < cards.size()) {
			// 更新客户端状态
			menu.selectedCardIndex = index;
			currentCard = cards.get(index);
			currentPageIndex = 0;

			// 通知服务端
			CombatDepot.PACKET_HANDLER.sendToServer(new CardSelectionMessage(index));

			// 加载新选中卡片的第一页
			loadCurrentPage();

			// 播放音效
			if (minecraft != null && minecraft.player != null) {
				minecraft.player.playSound(SoundEvents.UI_BUTTON_CLICK.get(), 1.0F, 1.0F);
			}
		}
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

		// 只渲染可见的卡片
		for (int i = 0; i < Math.min(VISIBLE_CARDS, cards.size() - currentCardOffset); i++) {
			int cardIndex = i + currentCardOffset;
			ICard card = cards.get(cardIndex);
			int cardX = startX + i * (CARD_WIDTH + CARD_SPACING);

			// 渲染卡片
			boolean isSelected = cardIndex == menu.selectedCardIndex;
			card.render(guiGraphics, minecraft, cardX, startY, CARD_WIDTH, CARD_HEIGHT, isSelected);

			if (isMouseOverCard(mouseX, mouseY, cardX, startY)) {
				guiGraphics.renderTooltip(font,
						Component.translatable("gui." + CombatDepot.MODID + "." + card.getName()),
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


		addScrollButtons();

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

	private void addScrollButtons() {
		// 左箭头按钮
		leftArrowButton = Button.builder(Component.literal("<"), (button) -> scrollCards(-1))
				.pos(getCardStartX() + (VISIBLE_CARDS * (CARD_WIDTH + CARD_SPACING)), getCardStartY() + (CARD_HEIGHT - ARROW_HEIGHT) / 2)
				.size(ARROW_WIDTH, ARROW_HEIGHT)
				.build();

		// 右箭头按钮
		rightArrowButton = Button.builder(Component.literal(">"), (button) -> scrollCards(1))
				.pos(getCardStartX() + (VISIBLE_CARDS * (CARD_WIDTH + CARD_SPACING)) + ARROW_PADDING * 3,
						getCardStartY() + (CARD_HEIGHT - ARROW_HEIGHT) / 2)
				.size(ARROW_WIDTH, ARROW_HEIGHT)
				.build();

		this.addRenderableWidget(leftArrowButton);
		this.addRenderableWidget(rightArrowButton);

		updateArrowButtonsState();
	}

	private void scrollCards(int direction) {
		int newOffset = currentCardOffset + direction;
		if (newOffset >= 0 && newOffset <= Math.max(0, cards.size() - VISIBLE_CARDS)) {
			currentCardOffset = newOffset;
			updateArrowButtonsState();
		}
	}

	private void updateArrowButtonsState() {
		if (leftArrowButton != null) {
			leftArrowButton.active = currentCardOffset > 0;
		}
		if (rightArrowButton != null) {
			rightArrowButton.active = currentCardOffset < Math.max(0, cards.size() - VISIBLE_CARDS);
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
				Colors.WHITE);
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

    public GunViewMenu getContainer() {
        return container;
    }
}