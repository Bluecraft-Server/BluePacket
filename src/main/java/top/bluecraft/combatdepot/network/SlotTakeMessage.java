package top.bluecraft.combatdepot.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import top.bluecraft.combatdepot.client.menu.GunViewMenu;

import java.util.function.Supplier;

public record SlotTakeMessage(int slotIndex) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(slotIndex);
    }

    public static SlotTakeMessage decode(FriendlyByteBuf buf) {
        return new SlotTakeMessage(buf.readVarInt());
    }

    public static void handle(SlotTakeMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null && player.containerMenu instanceof GunViewMenu menu) {
                int index = message.slotIndex();
                if (index >= 0 && index < menu.slots.size()) {
                    // Remove the slot taken check to allow repeated item pickup
                    menu.markSlotTaken(index);
                    menu.broadcastChanges();
                }
            }
        });
        context.setPacketHandled(true);
    }
}