package top.bluecraft.combatdepot.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import top.bluecraft.combatdepot.CombatDepot;
import top.bluecraft.combatdepot.api.ICard;
import top.bluecraft.combatdepot.client.CardRenderer;
import top.bluecraft.combatdepot.client.Colors;
import top.bluecraft.combatdepot.common.inventory.menu.GunViewMenu;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class GunViewScreen extends AbstractContainerScreen<GunViewMenu> {
	// Region: Constants
	public static final int IMAGE_WIDTH = 400;
	public static final int IMAGE_HEIGHT = 200;
	private static final int CARD_WIDTH = 32;
	private static final int CARD_HEIGHT = 16;
	private static final int CARD_SPACING = 8;
	private static final int PAGE_INFO_Y_OFFSET = 20;
	private static final int INVENTORY_LABEL_X = 220;
	private static final int INVENTORY_LABEL_Y = 98;
	private static final int CARD_NAME_X = 14;
	private static final int CARD_NAME_Y = 5;
	private static final int VISIBLE_CARDS = 8;
	private static final int ARROW_WIDTH = 12;
	private static final int ARROW_HEIGHT = 12;
	private static final int ARROW_PADDING = 4;

	private static final ResourceLocation TEXTURE = new ResourceLocation(CombatDepot.MODID, "textures/gui/gun_view.png");

	// Region: Fields
	private final List<ICard> cards = new ArrayList<>();
	private ICard currentCard;
	private int currentPageIndex = 0;
	private int currentCardOffset = 0;
	private Button leftArrowButton;
	private Button rightArrowButton;

	// Region: Constructor
	public GunViewScreen(GunViewMenu container, Inventory inventory, Component text) {
		super(container, inventory, text);
		this.imageWidth = IMAGE_WIDTH;
		this.imageHeight = IMAGE_HEIGHT;
		initializeCards();
		this.currentCard = !cards.isEmpty() ? cards.get(0) : null;
	}

	public void updateCards(List<ICard> newCards) {
		this.cards.clear();
		this.cards.addAll(newCards);

		// 保持当前选择状态
		if (!cards.isEmpty()) {
			int prevSelected = Math.min(menu.getSelectedCardIndex(), cards.size()-1);
			this.currentCard = cards.get(prevSelected);
		}
		updateArrowButtonsState();
	}

	private void updateArrowButtonsState() {
		if (leftArrowButton != null) {
			leftArrowButton.active = currentCardOffset > 0;
		}
		if (rightArrowButton != null) {
			rightArrowButton.active = currentCardOffset < Math.max(0, cards.size() - VISIBLE_CARDS);
		}
	}


	// Region: Initialization
	private void initializeCards() {
		// 从菜单获取已初始化的卡片数据
		this.cards.addAll(menu.getCards());

		// 设置默认卡片
		if (!cards.isEmpty() && menu.getSelectedCardIndex() >= 0) {
			this.currentCard = cards.get(menu.getSelectedCardIndex());
		}

		if (!cards.isEmpty() && menu.getSelectedCardIndex() >= 0) {
			this.currentCard = cards.get(menu.getSelectedCardIndex());
			this.currentCardOffset = menu.getCardOffset(); // 加载偏移量
		}
	}

	public void updateCardSelection(int newIndex) {
		if (newIndex >= 0 && newIndex < cards.size()) {
			currentCard = cards.get(newIndex);
			currentPageIndex = 0;
			menu.selectCard(newIndex);
			playSelectSound();
		}
	}

	// Region: Rendering
	@Override
	public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		super.render(guiGraphics, mouseX, mouseY, partialTicks);
		renderCards(guiGraphics, mouseX, mouseY);
		renderCurrentCardInfo(guiGraphics);
		renderTooltip(guiGraphics, mouseX, mouseY);
	}

	@Override
	protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
		guiGraphics.drawString(this.font,
				Component.translatable("container.inventory"),
				INVENTORY_LABEL_X,
				INVENTORY_LABEL_Y,
				Colors.WHITE);
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, float v, int i, int i1) {
		RenderSystem.setShaderColor(1, 1, 1, 1);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
		RenderSystem.disableBlend();
	}

	private void renderCards(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		int startX = getCardStartX();
		int startY = getCardStartY();

		for (int i = 0; i < Math.min(VISIBLE_CARDS, cards.size() - currentCardOffset); i++) {
			int cardIndex = i + currentCardOffset;
			ICard card = cards.get(cardIndex);
			int cardX = startX + i * (CARD_WIDTH + CARD_SPACING);

			boolean isSelected = cardIndex == menu.getSelectedCardIndex();
			CardRenderer.render(card, guiGraphics, minecraft, cardX, startY, CARD_WIDTH, CARD_HEIGHT, isSelected);

			if (isMouseOverCard(mouseX, mouseY, cardX, startY)) {
				guiGraphics.renderTooltip(font,
						Component.translatable("gui." + CombatDepot.MODID + "." + card.getName()),
						mouseX, mouseY);
			}
		}
	}

	private void renderCurrentCardInfo(GuiGraphics guiGraphics) {
		if (currentCard != null) {
			CardRenderer.renderPageInfo(currentCard, menu, guiGraphics, font,
					leftPos + imageWidth / 2 + imageWidth / 4,
					topPos + PAGE_INFO_Y_OFFSET);
		}
	}

	// Region: Interaction
	@Override
	protected void init() {
		super.init();
		initNavigationButtons();
		initConfigButton();
	}

	private void initNavigationButtons() {
		if (currentCard != null) {
			CardRenderer.addPageButtons(menu, currentCard, this, width / 2, height - 40);
			addScrollButtons();
		}
	}

	private void initConfigButton() {
		if (hasConfigPermission()) {
			Button configButton = Button.builder(Component.translatable("gui." + CombatDepot.MODID + ".config"),
							(button) -> {
								if (minecraft != null) {
									minecraft.setScreen(new CardConfigScreen(this));
								}
							})
					.pos(leftPos, topPos - 20)
					.size(40, 20)
					.build();
			this.addRenderableWidget(configButton);
		}
	}

	// Region: Helper Methods
	private boolean isCardSelected(int index) {
		return index == menu.getSelectedCardIndex();
	}

	private void renderCardTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY, ICard card) {
		guiGraphics.renderTooltip(font,
				Component.translatable("tooltip." + CombatDepot.MODID + "." + card.getName()),
				mouseX, mouseY);
	}

	private void addScrollButtons() {
		leftArrowButton = Button.builder(Component.literal("<"), button -> {
					scrollCards(-1);
					menu.setCardOffset(currentCardOffset); // 同步到菜单
				})
				.pos(getCardStartX() + (VISIBLE_CARDS * (CARD_WIDTH + CARD_SPACING)),
						getCardStartY() + (CARD_HEIGHT - ARROW_HEIGHT) / 2)
				.size(ARROW_WIDTH, ARROW_HEIGHT)
				.build();

		rightArrowButton = Button.builder(Component.literal(">"), button -> {
					scrollCards(1);
					menu.setCardOffset(currentCardOffset); // 同步到菜单
				})
				.pos(getCardStartX() + (VISIBLE_CARDS * (CARD_WIDTH + CARD_SPACING)) + ARROW_PADDING * 3,
						getCardStartY() + (CARD_HEIGHT - ARROW_HEIGHT) / 2)
				.size(ARROW_WIDTH, ARROW_HEIGHT)
				.build();

		this.addRenderableWidget(leftArrowButton);
		this.addRenderableWidget(rightArrowButton);
		updateArrowButtons();
	}

	private void updateArrowButtons() {
		leftArrowButton.active = currentCardOffset > 0;
		rightArrowButton.active = currentCardOffset < Math.max(0, cards.size() - VISIBLE_CARDS);
	}

	private int getCardStartX() {
		return leftPos + 42;
	}

	private int getCardStartY() {
		return topPos - 16;
	}

	private boolean hasConfigPermission() {
		return minecraft != null && minecraft.player != null && minecraft.player.hasPermissions(2);
	}

	private void scrollCards(int direction) {
		int newOffset = currentCardOffset + direction;
		CombatDepot.LOGGER.debug("Scrolling cards: direction={}, newOffset={}", direction, newOffset);
		if (newOffset >= 0 && newOffset <= cards.size() - VISIBLE_CARDS) {
			currentCardOffset = newOffset;
			updateArrowButtons();
			menu.setCardOffset(currentCardOffset); // 同步到菜单
		}
	}
	public void setCardOffset(int offset) {
		menu.setCardOffset(offset);
	}

	private void playSelectSound() {
		if (minecraft != null && minecraft.player != null) {
			minecraft.player.playSound(SoundEvents.UI_BUTTON_CLICK.get(), 0.5F, 1.0F);
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0 && handleCardSelection(mouseX, mouseY)) {
			return true;
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	private boolean handleCardSelection(double mouseX, double mouseY) {
		int startX = getCardStartX();
		int startY = getCardStartY();

		for (int i = 0; i < Math.min(VISIBLE_CARDS, cards.size() - currentCardOffset); i++) {
			int cardIndex = i + currentCardOffset;
			int cardX = startX + i * (CARD_WIDTH + CARD_SPACING);

			if (isMouseOverCard(mouseX, mouseY, cardX, startY) && !isCardSelected(cardIndex)) {
				updateCardSelection(cardIndex);
				return true;
			}
		}
		return false;
	}

	private boolean isMouseOverCard(double mouseX, double mouseY, int cardX, int cardY) {
		return mouseX >= cardX && mouseX < cardX + CARD_WIDTH &&
				mouseY >= cardY && mouseY < cardY + CARD_HEIGHT;
	}

    public int getCurrentPageIndex() {
        return currentPageIndex;
    }
}