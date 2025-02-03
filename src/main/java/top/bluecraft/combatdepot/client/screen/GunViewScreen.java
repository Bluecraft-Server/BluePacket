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
import top.bluecraft.combatdepot.client.CardRenderer;
import top.bluecraft.combatdepot.client.Colors;
import top.bluecraft.combatdepot.common.inventory.menu.GunViewMenu;
import top.bluecraft.combatdepot.common.card.Cards;
import top.bluecraft.combatdepot.common.card.GeneralCard;
import top.bluecraft.combatdepot.common.page.Page;
import top.bluecraft.combatdepot.network.CardSelectionMessage;
import top.bluecraft.combatdepot.network.LoadPageMessage;

@OnlyIn(Dist.CLIENT)
public class GunViewScreen extends AbstractContainerScreen<GunViewMenu> {
	public static final int IMAGE_WIDTH = 400;
	public static final int IMAGE_HEIGHT = 200;
	private static final int CARD_WIDTH = 32;
	private static final int CARD_HEIGHT = 16;
	private static final int CARD_SPACING = 8;
	private static final int CARDS_PER_ROW = 5;
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
	private static final ResourceLocation ARROWS_TEXTURE = new ResourceLocation(CombatDepot.MODID, "textures/gui/arrows.png");

	private final Level world;
	private final int x, y, z;
	private final Player entity;
	private final List<ICard> cards = new ArrayList<>();
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
		this.entity = container.entity;
		this.imageWidth = IMAGE_WIDTH;
		this.imageHeight = IMAGE_HEIGHT;
		initializeCards();
		this.currentCard = !cards.isEmpty() ? cards.get(0) : null;
	}

	private void initializeCards() {
		for (ICardInventory inventory : menu.getInventories()) {
			cards.add(new GeneralCard(
					inventory,
					inventory.getName(),
					CombatDepot.MODID + ":textures/gui/cards/" + inventory.getName() + ".png",
					this::onPageChanged
			));
		}
	}

	public void updateCardSelection(int newIndex) {
		if (newIndex >= 0 && newIndex < cards.size()) {
			currentCard = cards.get(newIndex);
			currentPageIndex = 0;
			menu.requestCardSelection(newIndex);
			// 播放选择音效
			if (minecraft != null && minecraft.player != null) {
				minecraft.player.playSound(SoundEvents.UI_BUTTON_CLICK.get(), 1.0F, 1.0F);
			}
		}
	}

	private void onPageChanged(int newPageIndex) {
		if (currentCard != null && newPageIndex >= 0 && newPageIndex < currentCard.getTotalPages()) {
			currentPageIndex = newPageIndex;
			menu.requestPageLoad(currentPageIndex, currentCard.getPage(currentPageIndex).items());
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

			if (isMouseOverCard(mouseX, mouseY, cardX, startY)) {
				if (menu.getSelectedCardIndex() != cardIndex) {
					menu.requestCardSelection(cardIndex);
					playSelectSound();
					currentCard = cards.get(cardIndex);
					currentPageIndex = 0;
					return true;
				}
			}
		}
		return false;
	}

	private void playSelectSound() {
		if (minecraft != null && minecraft.player != null) {
			minecraft.player.playSound(SoundEvents.UI_BUTTON_CLICK.get(), 1.0F, 1.0F);
		}
	}

	@Override
	public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		this.renderBackground(guiGraphics);
		super.render(guiGraphics, mouseX, mouseY, partialTicks);
		renderCards(guiGraphics, mouseX, mouseY);
		renderCurrentCardInfo(guiGraphics);
		this.renderTooltip(guiGraphics, mouseX, mouseY);
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
			CardRenderer.renderPageInfo(currentCard, guiGraphics, font,
					leftPos + imageWidth / 2 + imageWidth / 4,
					topPos + PAGE_INFO_Y_OFFSET);
		}
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int gx, int gy) {
		RenderSystem.setShaderColor(1, 1, 1, 1);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
		RenderSystem.disableBlend();
	}

	@Override
	protected void init() {
		super.init();
		addScrollButtons();

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

		if (currentCard != null) {
			CardRenderer.addPageButtons(currentCard, this, width / 2, height - 40);
		}
	}

	private void addScrollButtons() {
		leftArrowButton = Button.builder(Component.literal("<"), (button) -> scrollCards(-1))
				.pos(getCardStartX() + (VISIBLE_CARDS * (CARD_WIDTH + CARD_SPACING)),
						getCardStartY() + (CARD_HEIGHT - ARROW_HEIGHT) / 2)
				.size(ARROW_WIDTH, ARROW_HEIGHT)
				.build();

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
		if (key == 256) {
			if (minecraft != null && minecraft.player != null) {
				minecraft.player.closeContainer();
			}
			return true;
		}
		return super.keyPressed(key, b, c);
	}

	@Override
	protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
		guiGraphics.drawString(this.font,
				Component.translatable("container.inventory"),
				INVENTORY_LABEL_X,
				INVENTORY_LABEL_Y,
				Colors.WHITE);
	}

	private boolean hasConfigPermission() {
		return minecraft != null && minecraft.player != null &&
				minecraft.player.hasPermissions(2);
	}

	private int getCardStartX() {
		return leftPos + 42;
	}

	private int getCardStartY() {
		return topPos - 16;
	}

	public List<ICard> getCards() {
		return cards;
	}
}