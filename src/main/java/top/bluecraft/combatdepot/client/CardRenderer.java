package top.bluecraft.combatdepot.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import top.bluecraft.combatdepot.CombatDepot;
import top.bluecraft.combatdepot.api.ICard;
import top.bluecraft.combatdepot.common.inventory.menu.CombatDepotMenu;

public class CardRenderer {
    public static final ResourceLocation BG_RESOURCE = new ResourceLocation(CombatDepot.MODID, "textures/gui/general.png");
    private static final int PAGE_INFO_COLOR = 0xFFFFFF;
    private static final int ARROW_SPACING = 15;

    public static void render(ICard card, GuiGraphics graphics, Minecraft minecraft, int x, int y, int width, int height, boolean isSelected) {
        if (minecraft == null) return;

        try {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            // 渲染背景
            graphics.blit(BG_RESOURCE, x, y, 0, 0, width, height, width, height);// 渲染overlay
            graphics.blit(card.getTexture(), x, y, 0, 0, width, height, width, height);

            // 如果被选中，渲染高亮效果
            if (isSelected) {
                graphics.fill(x, y, x + width, y + height, 0x80FFFFFF);
            }

            RenderSystem.disableBlend();
        } catch (Exception e) {
            CombatDepot.LOGGER.error("Failed to render card: {}", card.getName(), e);
        }
    }

    public static void renderPageInfo(ICard card, CombatDepotMenu menu, GuiGraphics graphics, Font font, int centerX, int y) {
        // 渲染页码
        Component pageInfo = Component.translatable("gui." + CombatDepot.MODID + ".page", menu.getCurrentPage() + 1, card.getTotalPages());
        graphics.drawCenteredString(font, pageInfo, centerX, y, PAGE_INFO_COLOR);

        // 渲染卡片名称
        graphics.drawCenteredString(font, Component.translatable("gui." + CombatDepot.MODID + "." + card.getName()), centerX, y - 15, PAGE_INFO_COLOR);

    }

    public static void addPageButtons(CombatDepotMenu menu, ICard card, Screen screen, int centerX, int y) {
        // 箭头按钮的Y坐标
        int arrowY = y + 20;

        // 左箭头按钮
        screen.addRenderableWidget(Button.builder(Component.literal("<"), button -> {
                    if (menu.getCurrentPage() > 0) {
                        menu.setPage(menu.getCurrentPage() - 1);
                    }
                })
                .pos(centerX - ARROW_SPACING - 10, arrowY - 10)  // 按钮位置，调整-10使按钮居中
                .size(20, 20)  // 按钮大小
                .build());


        // 右箭头按钮
        screen.addRenderableWidget(Button.builder(Component.literal(">"), button -> {
                    if (menu.getCurrentPage() < card.getTotalPages() - 1) {
                        menu.setPage(menu.getCurrentPage() + 1);
                    }
                })
                .pos(centerX + ARROW_SPACING - 10, arrowY - 10)
                .size(20, 20)
                .build());
    }

    private boolean isMouseOverArrow(double mouseX, double mouseY, int arrowX, int arrowY) {
        // 增大点击判定区域以便于点击
        int hitboxSize = 10;
        return mouseX >= arrowX - hitboxSize && mouseX <= arrowX + hitboxSize &&
                mouseY >= arrowY - hitboxSize && mouseY <= arrowY + hitboxSize;
    }
}
