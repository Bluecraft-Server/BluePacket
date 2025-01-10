package top.bluecraft.bluepacket.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

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

    public GunViewChangeMessage(FriendlyByteBuf buffer) {
        this.pageIndex = buffer.readInt();
        this.totalPages = buffer.readInt();
        this.slotID = buffer.readInt();
        this.x = buffer.readInt();
        this.y = buffer.readInt();
        this.z = buffer.readInt();
        this.changeType = buffer.readInt();
        this.meta = buffer.readInt();
    }

    // 编码包数据
    public void buffer(FriendlyByteBuf buffer) {
        buffer.writeInt(pageIndex);
        buffer.writeInt(totalPages);
        buffer.writeInt(slotID);
        buffer.writeInt(x);
        buffer.writeInt(y);
        buffer.writeInt(z);
        buffer.writeInt(changeType);
        buffer.writeInt(meta);
    }

    // 处理包数据
    public static void handle(GunViewChangeMessage packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            // 在服务端处理页面切换
            System.out.println("Page changed to: " + packet.pageIndex);
            System.out.println("Total pages: " + packet.totalPages);
            Player entity = context.get().getSender();
            int slotID = packet.slotID;
            int changeType = packet.changeType;
            int meta = packet.meta;
            int x = packet.x;
            int y = packet.y;
            int z = packet.z;
        });
        context.get().setPacketHandled(true);
    }
}
