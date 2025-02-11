package top.bluecraft.combatdepot.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import top.bluecraft.combatdepot.common.inventory.menu.CombatDepotMenu;

import java.util.function.Supplier;

public class UpdateCardSelectionMessage {
    private final int selectedIndex;

    public UpdateCardSelectionMessage(int selectedIndex) {
        this.selectedIndex = selectedIndex;
    }

    public static void encode(UpdateCardSelectionMessage msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.selectedIndex);
    }

    public static UpdateCardSelectionMessage decode(FriendlyByteBuf buf) {
        return new UpdateCardSelectionMessage(buf.readVarInt());
    }

    public static void handle(UpdateCardSelectionMessage msg, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null && player.containerMenu instanceof CombatDepotMenu menu) {
                menu.selectCard(msg.selectedIndex);
                // 强制同步整个容器状态
                menu.syncDisplayInventory();
            }
        });
        context.setPacketHandled(true);
    }
}