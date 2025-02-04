package top.bluecraft.combatdepot.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import top.bluecraft.combatdepot.CombatDepot;
import top.bluecraft.combatdepot.common.data.GlobalCardStorage;
import top.bluecraft.combatdepot.common.inventory.menu.GunViewMenu;
import top.bluecraft.combatdepot.config.CardConfig;

import java.util.List;
import java.util.function.Supplier;

public class RequestCardsPacket {
    public RequestCardsPacket() {
    }

    public RequestCardsPacket(FriendlyByteBuf buf) {
    }

    public void encode(FriendlyByteBuf buf) {
    }

    public static RequestCardsPacket decode(FriendlyByteBuf buf) {
        return new RequestCardsPacket();
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                // 获取服务端数据
                GlobalCardStorage storage = GlobalCardStorage.get(player.serverLevel());
                CardConfig config = CardConfig.load();
                config.loadFromGlobalStorage(player, storage);
                List<GunViewMenu.Card> cards = config.createCards();

                // 发送数据回客户端
                CombatDepot.PACKET_HANDLER.send(
                        PacketDistributor.PLAYER.with(() -> player),
                        new SyncCardsPacket(cards, 0) // 0 是初始偏移量
                );
            }
        });
        ctx.get().setPacketHandled(true);
    }
}