package top.bluecraft.viewlauncher.network;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;
import top.bluecraft.viewlauncher.CombatDepot;
import top.bluecraft.viewlauncher.api.ICard;
import top.bluecraft.viewlauncher.client.menu.GunViewMenu;
import top.bluecraft.viewlauncher.client.screen.GunViewScreen;

import java.util.List;
import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
public class ClientboundCardSelectionPacket {
    private final int selectedIndex;

    public ClientboundCardSelectionPacket(int selectedIndex) {
        this.selectedIndex = selectedIndex;
    }

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
                List<ICard> cards = screen.getCards();
                if (packet.selectedIndex >= 0 && packet.selectedIndex < cards.size()) {
                    // 更新客户端UI
                    screen.updateCardSelection(packet.selectedIndex);
                }
            }
        });
        context.setPacketHandled(true);
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }
}