package top.bluecraft.combatdepot.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import top.bluecraft.combatdepot.CombatDepot;
import top.bluecraft.combatdepot.api.ICard;
import top.bluecraft.combatdepot.common.inventory.menu.CombatDepotMenu;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class CardEditScreen extends Screen {
    private static final int ITEMS_PER_PAGE = 27;
    private final CardConfigScreen parentScreen;
    private final ICard card;
    private EditBox itemInput;
    private int currentPage = 0;

    public CardEditScreen(CardConfigScreen parentScreen, ICard card) {
        super(Component.translatable("gui." + CombatDepot.MODID + ".card.edit",
                Component.translatable("gui." + CombatDepot.MODID + "." + card.getName())));
        this.parentScreen = parentScreen;
        this.card = card;
    }

    @Override
    protected void init() {
        super.init();

        // 添加页面导航按钮
        Button prevButton = Button.builder(
                        Component.literal("<"),
                        button -> {
                            if (currentPage > 0) {
                                currentPage--;
                            }
                        })
                .pos(width / 2 - 100, height / 2 - 10)
                .size(20, 20)
                .build();

        Button nextButton = Button.builder(
                        Component.literal(">"),
                        button -> {
                            if (parentScreen.getMinecraft().player != null &&
                                    parentScreen.getMinecraft().player.containerMenu instanceof CombatDepotMenu menu) {
                                int totalItems = menu.getCurrentCard().getInventory().size();
                                int totalPages = (int) Math.ceil((double) totalItems / ITEMS_PER_PAGE);
                                if (currentPage < totalPages - 1) {
                                    currentPage++;
                                }
                            }
                        })
                .pos(width / 2 + 80, height / 2 - 10)
                .size(20, 20)
                .build();

        addRenderableWidget(prevButton);
        addRenderableWidget(nextButton);

        // 原有的按钮
        addRenderableWidget(Button.builder(
                        Component.translatable("gui." + CombatDepot.MODID + ".card.edit.config"),
                        button -> {
                            if (minecraft != null) {
                                minecraft.setScreen(new CardItemConfigScreen(this, card));
                            }
                        })
                .pos(width / 2 - 60, height - 50)
                .size(120, 20)
                .build());

        addRenderableWidget(Button.builder(
                        Component.translatable("gui." + CombatDepot.MODID + ".return"),
                        button -> {
                            if (minecraft != null) {
                                minecraft.setScreen(parentScreen);
                            }
                        })
                .pos(width / 2 - 200, height - 50)
                .size(100, 20)
                .build());
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTicks);

        graphics.drawCenteredString(font, title, width / 2, 20, 0xFFFFFF);

        Component pageInfo = Component.translatable("gui." + CombatDepot.MODID + ".page",
                currentPage + 1, card.getTotalPages());
        graphics.drawCenteredString(font, pageInfo, width / 2, height / 2 - 10, 0xFFFFFF);

        renderCardPage(graphics);
    }

    private void renderCardPage(GuiGraphics graphics) {
        if (parentScreen.getMinecraft().player != null &&
                parentScreen.getMinecraft().player.containerMenu instanceof CombatDepotMenu menu) {
            List<ItemStack> allItems = new ArrayList<>(card.getInventory());

            int startIndex = currentPage * ITEMS_PER_PAGE;
            int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, allItems.size());
            List<ItemStack> pageItems = allItems.subList(startIndex, endIndex);

            int startX = width / 2 - 90;
            int startY = 50;
            int itemSize = 16;
            int spacing = 20;

            for (int i = 0; i < pageItems.size(); i++) {
                int row = i / 9;
                int col = i % 9;
                int x = startX + col * spacing;
                int y = startY + row * spacing;

                ItemStack stack = pageItems.get(i);
                if (!stack.isEmpty()) {
                    graphics.renderItem(stack, x, y);
                    graphics.renderItemDecorations(font, stack, x, y);
                }
            }
        }
    }

    public CardConfigScreen getParentScreen() {
        return parentScreen;
    }
}