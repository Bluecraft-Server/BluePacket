package top.bluecraft.viewlauncher.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import top.bluecraft.viewlauncher.ViewLauncher;
import top.bluecraft.viewlauncher.api.ICard;

import java.util.List;

public class CardConfigScreen extends Screen {
    private final GunViewScreen parentScreen;
    private static final int BUTTONS_PER_ROW = 2;
    private static final int BUTTON_WIDTH = 100;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_SPACING = 10;

    public CardConfigScreen(GunViewScreen parentScreen) {
        super(Component.translatable("gui." + ViewLauncher.MODID + ".card.config"));
        this.parentScreen = parentScreen;
    }

    @Override
    public void onClose() {
        if (minecraft != null && minecraft.player != null) {
            // 如果玩家失去权限，强制返回主界面
            if (!hasConfigPermission()) {
                minecraft.setScreen(parentScreen);
                return;
            }
        }
        super.onClose();
    }

    private boolean hasConfigPermission() {
        if (minecraft == null || minecraft.player == null) return false;

        return minecraft.player.hasPermissions(2);
    }

    @Override
    protected void init() {
        super.init();

        List<ICard> cards = parentScreen.getMenu().cards;
        int startX = (width - (BUTTON_WIDTH * BUTTONS_PER_ROW + BUTTON_SPACING)) / 2;
        int startY = 50;

        for (int i = 0; i < cards.size(); i++) {
            ICard card = cards.get(i);
            int row = i / BUTTONS_PER_ROW;
            int col = i % BUTTONS_PER_ROW;

            int x = startX + col * (BUTTON_WIDTH + BUTTON_SPACING);
            int y = startY + row * (BUTTON_HEIGHT + BUTTON_SPACING);

            addRenderableWidget(Button.builder(Component.translatable("gui." + ViewLauncher.MODID + "." + card.getName()), (button) -> {
                        if (minecraft != null) {
                            minecraft.setScreen(new CardEditScreen(this, card));
                        }
                    })
                    .pos(x, y)
                    .size(BUTTON_WIDTH, BUTTON_HEIGHT)
                    .build());
        }

        // 添加返回按钮
        addRenderableWidget(Button.builder(Component.translatable("gui." + ViewLauncher.MODID + ".return"), (button) -> {
                    if (minecraft != null) {
                        minecraft.setScreen(parentScreen);
                    }
                })
                .pos(width / 2 - 50, height - 30)
                .size(100, 20)
                .build());
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTicks);

        graphics.drawCenteredString(font, title, width / 2, 20, 0xFFFFFF);
    }
}
