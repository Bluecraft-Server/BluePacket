package top.bluecraft.bluepacket.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;
import top.bluecraft.bluepacket.client.menu.GunViewMenu;

import java.util.function.Supplier;

public class GunViewChangeMessage {
    private final int pageIndex, totalPages, slotID, x, y, z, changeType, meta;

    public GunViewChangeMessage(int pageIndex, int totalPages, int slotID, int x, int y, int z, int changeType, int meta) {
        this.pageIndex = pageIndex;
        this.totalPages = totalPages;
        this.slotID = slotID;
        this.x = x;
        this.y = y;
        this.z = z;
        this.changeType = changeType;
        this.meta = meta;
    }

    // 将构造函数改为静态decode方法
    public static GunViewChangeMessage decode(FriendlyByteBuf buffer) {
        return new GunViewChangeMessage(
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt()
        );
    }

    // 将buffer方法改名为encode
    public static void encode(GunViewChangeMessage message, FriendlyByteBuf buffer) {
        buffer.writeInt(message.pageIndex);
        buffer.writeInt(message.totalPages);
        buffer.writeInt(message.slotID);
        buffer.writeInt(message.x);
        buffer.writeInt(message.y);
        buffer.writeInt(message.z);
        buffer.writeInt(message.changeType);
        buffer.writeInt(message.meta);
    }

    public static void handle(GunViewChangeMessage message, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player != null && player.containerMenu instanceof GunViewMenu menu) {
                // 在这里处理消息
                System.out.println("Page changed to: " + message.pageIndex);
                System.out.println("Total pages: " + message.totalPages);
                // ... 其他处理逻辑
            }
        });
        context.get().setPacketHandled(true);
    }
}