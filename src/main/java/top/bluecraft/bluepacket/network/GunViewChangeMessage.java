package top.bluecraft.bluepacket.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class GunViewChangeMessage {
    private final int pageIndex;
    private final int totalPages;

    public GunViewChangeMessage(int pageIndex, int totalPages) {
        this.pageIndex = pageIndex;
        this.totalPages = totalPages;
    }

    public GunViewChangeMessage(FriendlyByteBuf buffer) {
        this.pageIndex = buffer.readInt();
        this.totalPages = buffer.readInt();
    }

    // 编码包数据
    public void buffer(FriendlyByteBuf buffer) {
        buffer.writeInt(pageIndex);
        buffer.writeInt(totalPages);
    }

    // 处理包数据
    public static void handle(GunViewChangeMessage packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            // 在服务端处理页面切换
            System.out.println("Page changed to: " + packet.pageIndex);
            System.out.println("Total pages: " + packet.totalPages);
        });
        context.get().setPacketHandled(true);
    }
}
