package top.bluecraft.combatdepot.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import top.bluecraft.combatdepot.common.inventory.menu.GunViewMenu;

import java.util.function.Supplier;

public record UpdateSlotMessage(String cardName, int slotIndex, ItemStack stack) {
    public static void encode(UpdateSlotMessage msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.cardName());
        buf.writeVarInt(msg.slotIndex());
        buf.writeItem(msg.stack());
    }

    public static UpdateSlotMessage decode(FriendlyByteBuf buf) {
        return new UpdateSlotMessage(
                buf.readUtf(),
                buf.readVarInt(),
                buf.readItem()
        );
    }

    public static void handle(UpdateSlotMessage msg, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null && player.containerMenu instanceof GunViewMenu menu) {
                GunViewMenu.Card card = menu.getCards().stream()
                        .filter(c -> c.getName().equals(msg.cardName()))
                        .findFirst()
                        .orElse(null);

                if (card != null && msg.slotIndex() >= 0 && msg.slotIndex() < card.getInventory().size()) {
                    card.getInventory().set(msg.slotIndex(), msg.stack());
                    menu.savePersistentData(); // 保存到全局存储
                }
            }
        });
        context.setPacketHandled(true);
    }
}