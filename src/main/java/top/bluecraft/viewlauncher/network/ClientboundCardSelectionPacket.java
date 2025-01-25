package top.bluecraft.viewlauncher.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;
import top.bluecraft.viewlauncher.CombatDepot;
import top.bluecraft.viewlauncher.client.menu.GunViewMenu;
import top.bluecraft.viewlauncher.client.screen.GunViewScreen;

import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
public class ClientboundCardSelectionPacket{
    private final int selectedIndex;
    private final int x, y, z;

    public ClientboundCardSelectionPacket(CardSelectionMessage message) {
        this.selectedIndex = message.selectedIndex();
        this.x = message.x();
        this.y = message.y();
        this.z = message.z();
    }

    public ClientboundCardSelectionPacket(int selectedIndex, int x, int y, int z) {
        this.selectedIndex = selectedIndex;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static void encode(ClientboundCardSelectionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.selectedIndex);
        buffer.writeInt(packet.x);
        buffer.writeInt(packet.y);
        buffer.writeInt(packet.z);
    }

    public static ClientboundCardSelectionPacket decode(FriendlyByteBuf buffer) {
        return new ClientboundCardSelectionPacket(
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt()
        );
    }

    public static class Handler {
        public static void handle(ClientboundCardSelectionPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                // 确保我们在客户端
                if (!context.getDirection().getReceptionSide().isClient()) return;

                Minecraft minecraft = Minecraft.getInstance();
                Player player = minecraft.player;
                if (player == null) return;

                // 获取当前打开的容器
                if (!(player.containerMenu instanceof GunViewMenu menu)) return;

                // 验证卡片索引
                if (packet.selectedIndex < 0 || packet.selectedIndex >= menu.cards.size()) {
                    CombatDepot.LOGGER.warn("Invalid card index received from server: {}", packet.selectedIndex);
                    return;
                }

                // 更新客户端的选择
                menu.selectedCardIndex = packet.selectedIndex;
                menu.currentCard = menu.cards.get(packet.selectedIndex);

                // 重置页面索引并加载第一页
                menu.currentPageIndex = 0;
                if (menu.currentCard != null) {
                    menu.currentCard.switchToPage(0);
                    menu.loadCurrentPage();
                }

                // 如果需要，更新UI
                if (minecraft.screen instanceof GunViewScreen screen) {
                    screen.updateCardSelection(packet.selectedIndex);
                }
            });
            context.setPacketHandled(true);
        }
    }

    // Getters
    public int getSelectedIndex() {
        return selectedIndex;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }
}
