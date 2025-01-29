package top.bluecraft.combatdepot.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import top.bluecraft.combatdepot.api.ICardInventory;
import top.bluecraft.combatdepot.client.menu.GunViewMenu;

import java.util.List;
import java.util.function.Supplier;

public record CardSelectionMessage(int selectedIndex) {
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(selectedIndex);
    }

    public static CardSelectionMessage decode(FriendlyByteBuf buffer) {
        return new CardSelectionMessage(buffer.readVarInt());
    }

    public static void handle(CardSelectionMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null && player.containerMenu instanceof GunViewMenu menu) {
                // 验证索引
                List<ICardInventory> inventories = menu.getInventories();
                if (message.selectedIndex >= 0 && message.selectedIndex < inventories.size()) {
                    // 更新选择
                    menu.selectCard(message.selectedIndex);
                    // 广播更改
                    menu.broadcastChanges();
                }
            }
        });
        context.setPacketHandled(true);
    }
}