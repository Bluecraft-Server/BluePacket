package top.bluecraft.combatdepot.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import top.bluecraft.combatdepot.CombatDepot;
import top.bluecraft.combatdepot.common.inventory.menu.GunViewMenu;

import java.util.function.Supplier;

public record CardSelectionMessage(int cardId) {
    public static void encode(CardSelectionMessage msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.cardId);
    }

    public static CardSelectionMessage decode(FriendlyByteBuf buf) {
        return new CardSelectionMessage(buf.readVarInt());
    }

    public static void handle(CardSelectionMessage msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null && player.containerMenu instanceof GunViewMenu menu) {
                // 在服务端更新选择
                menu.selectCard(msg.cardId);

                // 同步回客户端
                CombatDepot.PACKET_HANDLER.send(
                        PacketDistributor.PLAYER.with(() -> player),
                        new ClientboundCardSelectionPacket(msg.cardId())
                );
            }
        });
        ctx.get().setPacketHandled(true);
    }
}