package top.bluecraft.combatdepot.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import top.bluecraft.combatdepot.CombatDepot;
import top.bluecraft.combatdepot.api.ICard;
import top.bluecraft.combatdepot.common.inventory.menu.GunViewMenu;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class CardConfigScreen extends Screen {
    private final GunViewScreen parentScreen;
    private static final int BUTTONS_PER_ROW = 2;
    private static final int BUTTON_WIDTH = 100;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_SPACING = 10;
    private static final int VISIBLE_ROWS = 5;  // 一次显示的行数
    private static final int CONTENT_TOP_MARGIN = 50;

    private int currentScroll = 0;
    private int maxScroll;
    private Button scrollUpButton;
    private Button scrollDownButton;

    public CardConfigScreen(GunViewScreen parentScreen) {
        super(Component.translatable("gui." + CombatDepot.MODID + ".card.config"));
        this.parentScreen = parentScreen;
    }

    @Override
    protected void init() {
        super.init();

        List<ICard> cards = parentScreen.getMenu().getCards();
        int totalRows = (int) Math.ceil((double) cards.size() / BUTTONS_PER_ROW);
        maxScroll = Math.max(0, totalRows - VISIBLE_ROWS);

        int contentWidth = (BUTTON_WIDTH * BUTTONS_PER_ROW + BUTTON_SPACING);
        int startX = (width - contentWidth) / 2;

        // 添加卡片按钮
        updateButtons();

        // 添加上下切换按钮
        addScrollButtons(startX + contentWidth + 10);

        // 添加返回按钮
        addRenderableWidget(Button.builder(
                        Component.translatable("gui." + CombatDepot.MODID + ".return"),
                        button -> {
                            if (minecraft != null) {
                                minecraft.setScreen(parentScreen);
                            }
                        })
                .pos(width / 2 - 50, height - 30)
                .size(100, 20)
                .build());
    }

    private void addScrollButtons(int xPosition) {
        // 上滚动按钮
        scrollUpButton = Button.builder(Component.literal("↑"), button -> scroll(-1))
                .pos(xPosition, CONTENT_TOP_MARGIN)
                .size(20, 20)
                .build();

        // 下滚动按钮
        scrollDownButton = Button.builder(Component.literal("↓"), button -> scroll(1))
                .pos(xPosition, CONTENT_TOP_MARGIN + VISIBLE_ROWS * (BUTTON_HEIGHT + BUTTON_SPACING))
                .size(20, 20)
                .build();

        addRenderableWidget(scrollUpButton);
        addRenderableWidget(scrollDownButton);
        updateScrollButtonsState();
    }

    private void updateButtons() {
        clearButtons();

        List<ICard> cards = parentScreen.getMenu().getCards();
        int startX = (width - (BUTTON_WIDTH * BUTTONS_PER_ROW + BUTTON_SPACING)) / 2;

        // 只添加可见范围内的按钮
        for (int i = currentScroll * BUTTONS_PER_ROW;
             i < Math.min(cards.size(), (currentScroll + VISIBLE_ROWS) * BUTTONS_PER_ROW);
             i++) {

            ICard card = cards.get(i);
            int localRow = (i / BUTTONS_PER_ROW) - currentScroll;
            int col = i % BUTTONS_PER_ROW;

            int x = startX + col * (BUTTON_WIDTH + BUTTON_SPACING);
            int y = CONTENT_TOP_MARGIN + localRow * (BUTTON_HEIGHT + BUTTON_SPACING);

            addRenderableWidget(Button.builder(
                            Component.translatable("gui." + CombatDepot.MODID + "." + card.getName()),
                            button -> {
                                if (minecraft != null) {
                                    minecraft.setScreen(new CardEditScreen(this, card));
                                }
                            })
                    .pos(x, y)
                    .size(BUTTON_WIDTH, BUTTON_HEIGHT)
                    .build());
        }
    }

    private void scroll(int direction) {
        int newScroll = currentScroll + direction;
        if (newScroll >= 0 && newScroll <= maxScroll) {
            currentScroll = newScroll;
            updateButtons();
            updateScrollButtonsState();
        }
    }

    private void updateScrollButtonsState() {
        if (scrollUpButton != null) {
            scrollUpButton.active = currentScroll > 0;
        }
        if (scrollDownButton != null) {
            scrollDownButton.active = currentScroll < maxScroll;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (delta != 0) {
            scroll(delta > 0 ? -1 : 1);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTicks);
        graphics.drawCenteredString(font, title, width / 2, 20, 0xFFFFFF);
    }

    private void clearButtons() {
        List<Button> buttonsToRemove = new ArrayList<>();
        Button returnButton = null;

        for (GuiEventListener widget : this.children()) {
            if (widget instanceof Button button) {
                if (button == scrollUpButton || button == scrollDownButton) {
                    continue;
                }
                if (button.getMessage().getString().equals(
                        Component.translatable("gui." + CombatDepot.MODID + ".return").getString())) {
                    returnButton = button;
                    continue;
                }
                buttonsToRemove.add(button);
            }
        }

        for (Button button : buttonsToRemove) {
            this.removeWidget(button);
        }

        if (returnButton != null) {
            addRenderableWidget(returnButton);
        }
    }

    private boolean hasConfigPermission() {
        return minecraft != null &&
                minecraft.player != null &&
                minecraft.player.hasPermissions(2);
    }

    public GunViewScreen getParentScreen() {
        return parentScreen;
    }
}