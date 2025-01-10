package top.bluecraft.bluepacket.common.card;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import top.bluecraft.bluepacket.BluePacket;

public class GeneralCard extends Card {
    public GeneralCard(Inventory inventory, ResourceLocation textures, String name) {
        super(inventory, textures, name);
    }

    @Override
    ResourceLocation backgroundResourceLocation() {
        return new ResourceLocation(BluePacket.MODID, "textures/gui/card.png");
    }

    @Override
    public void renderFont(GuiGraphics graphics, Font font, int x, int y) {
        super.renderFont(graphics, font, x, y);

        // 获取本地化名称
        Component localizedName = Component.translatable("card." + this.name()); // 假设name是本地化键的基础

        // 在指定的x, y位置渲染本地化文本
        graphics.drawCenteredString(font, localizedName, x, y - 20, 16777215);
    }

    @Override
    void initCard() {
        // 可以在这里进行初始化操作
    }
}
