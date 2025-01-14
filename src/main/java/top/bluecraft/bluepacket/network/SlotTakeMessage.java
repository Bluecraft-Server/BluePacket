package top.bluecraft.bluepacket.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import top.bluecraft.bluepacket.client.menu.GunViewMenu;

import java.util.function.Supplier;

public class SlotTakeMessage {
    private final int slotIndex;

    public SlotTakeMessage(int slotIndex) {
        this.slotIndex = slotIndex;
    }

    // Getter 方法
    public int getSlotIndex() {
        return slotIndex;
    }

    // 编码方法
    public static void encode(SlotTakeMessage message, FriendlyByteBuf buf) {
        buf.writeVarInt(message.slotIndex);
    }

    // 解码方法
    public static SlotTakeMessage decode(FriendlyByteBuf buf) {
        return new SlotTakeMessage(buf.readVarInt());
    }

    // 处理器
    public static class Handler {
        public static void handle(SlotTakeMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                // 确保在服务器端
                if (!context.getDirection().getReceptionSide().isServer()) {
                    return;
                }

                // 获取发送消息的玩家
                ServerPlayer player = context.getSender();
                if (player == null) {
                    return;
                }

                // 确保玩家正在使用正确的容器
                if (player.containerMenu instanceof GunViewMenu menu) {
                    int index = message.getSlotIndex();
                    // 检查槽位索引是否有效
                    if (index >= 0 && index < menu.slots.size()) {
                        // 标记槽位已被取出
                        menu.markSlotTaken(index);
                        // 广播更改到所有客户端
                        menu.broadcastChanges();
                    }
                }
            });
            context.setPacketHandled(true);
        }
    }
}