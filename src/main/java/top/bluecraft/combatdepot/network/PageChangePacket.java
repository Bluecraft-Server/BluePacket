package top.bluecraft.combatdepot.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import top.bluecraft.combatdepot.common.inventory.menu.GunViewMenu;

import java.util.function.Supplier;

public record PageChangePacket (String name, int page) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(name);
        buf.writeInt(page);
    }

    public static PageChangePacket decode(FriendlyByteBuf buf) {
        return new PageChangePacket(buf.readUtf(), buf.readInt());
    }

    public static void handle(PageChangePacket message, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null && player.containerMenu instanceof GunViewMenu menu) {
                menu.setPage(message.page);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
