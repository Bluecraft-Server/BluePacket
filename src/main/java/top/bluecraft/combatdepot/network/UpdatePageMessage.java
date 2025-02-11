package top.bluecraft.combatdepot.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import top.bluecraft.combatdepot.common.inventory.menu.CombatDepotMenu;

import java.util.function.Supplier;

public class UpdatePageMessage {
    private final int page;

    public UpdatePageMessage(int page) {
        this.page = page;
    }

    public static void encode(UpdatePageMessage msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.page);
    }

    public static UpdatePageMessage decode(FriendlyByteBuf buf) {
        return new UpdatePageMessage(buf.readVarInt());
    }

    public static void handle(UpdatePageMessage msg, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null && player.containerMenu instanceof CombatDepotMenu menu) {
                menu.setPage(msg.page);
            }
        });
        context.setPacketHandled(true);
    }
}