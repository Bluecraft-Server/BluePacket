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

import java.util.List;

@OnlyIn(Dist.CLIENT)
public class CardEditScreen extends Screen {
    private final CardConfigScreen parentScreen;
    private final ICard card;
    private EditBox itemInput;
    private int currentPage = 0;
    private static final int ITEMS_PER_PAGE = 27; // 每页显示27个物品

    public CardEditScreen(CardConfigScreen parentScreen, ICard card) {
        super(Component.translatable("gui." + CombatDepot.MODID + ".card.edit", card.getTranslatableComponent()));
        this.parentScreen = parentScreen;
        this.card = card;
    }

    @Override
    protected void init() {
        super.init();

        // 添加打开配置界面的按钮
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

        // 渲染标题
        graphics.drawCenteredString(font, title, width / 2, 20, 0xFFFFFF);

        // 渲染页面信息
        Component pageInfo = Component.translatable("gui." + CombatDepot.MODID + ".page", currentPage + 1, card.getTotalPages());
        graphics.drawCenteredString(font, pageInfo, width / 2, height / 2 - 10, 0xFFFFFF);

        // 渲染当前页面的物品
        renderCardPage(graphics);
    }

    private void renderCardPage(GuiGraphics graphics) {
        List<ItemStack> allItems = card.getPage(card.getCurrentPageIndex()).items();

        // 计算总页数
        int totalPages = (int) Math.ceil((double) allItems.size() / ITEMS_PER_PAGE);

        // 计算当前页的起始和结束索引
        int startIndex = currentPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, allItems.size());

        // 获取当前页要显示的物品
        List<ItemStack> pageItems = allItems.subList(startIndex, endIndex);

        int startX = width / 2 - 90;
        int startY = 50;
        int itemSize = 16;
        int spacing = 20;

        // 渲染当前页的物品（3行9列）
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
