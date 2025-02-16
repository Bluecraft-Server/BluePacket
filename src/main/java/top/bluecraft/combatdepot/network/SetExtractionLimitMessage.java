package top.bluecraft.combatdepot.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import top.bluecraft.combatdepot.common.data.GlobalCardStorage;
import top.bluecraft.combatdepot.common.inventory.Card;
import top.bluecraft.combatdepot.common.inventory.menu.CombatDepotMenu;

import java.util.function.Supplier;

public record SetExtractionLimitMessage(String cardName, int slotIndex, int limit) {
    public static void encode(SetExtractionLimitMessage msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.cardName());
        buf.writeVarInt(msg.slotIndex());
        buf.writeVarInt(msg.limit());
    }

    public static SetExtractionLimitMessage decode(FriendlyByteBuf buf) {
        return new SetExtractionLimitMessage(
                buf.readUtf(),
                buf.readVarInt(),
                buf.readVarInt()
        );
    }

    public static void handle(SetExtractionLimitMessage msg, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null && player.hasPermissions(2)) {
                ServerLevel level = player.serverLevel();
                GlobalCardStorage storage = GlobalCardStorage.get(level);

                Card card = storage.getInventory(msg.cardName());
                if (card != null) {
                    card.setExtractionLimit(msg.slotIndex(), msg.limit());
                    storage.updateInventory(msg.cardName(), card);
                    storage.setDirty();

                    // 同步更新后的数据到客户端
                    if (player.containerMenu instanceof CombatDepotMenu menu) {
                        menu.syncDisplayInventory();
                    }

                    // 发送确认消息回客户端
                    player.sendSystemMessage(Component.literal(
                            "Successfully set extraction limit to " + msg.limit()
                    ));
                }
            }
        });
        context.setPacketHandled(true);
    }
}
