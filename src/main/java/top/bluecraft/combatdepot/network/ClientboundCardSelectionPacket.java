package top.bluecraft.combatdepot.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;
import top.bluecraft.combatdepot.client.screen.GunViewScreen;
import top.bluecraft.combatdepot.common.inventory.menu.GunViewMenu;

import java.util.List;
import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
public record ClientboundCardSelectionPacket(int selectedIndex) {

    public static void encode(ClientboundCardSelectionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.selectedIndex);
    }

    public static ClientboundCardSelectionPacket decode(FriendlyByteBuf buffer) {
        return new ClientboundCardSelectionPacket(buffer.readVarInt());
    }

    public static void handle(ClientboundCardSelectionPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.screen instanceof GunViewScreen screen) {
                // 验证卡片索引
                List<GunViewMenu.Card> cards = screen.getMenu().getCards();
                if (packet.selectedIndex >= 0 && packet.selectedIndex < cards.size()) {
                    // 更新客户端UI
                    screen.updateCardSelection(packet.selectedIndex);
                }
            }
        });
        context.setPacketHandled(true);
    }
}