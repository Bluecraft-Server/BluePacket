package top.bluecraft.combatdepot.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import top.bluecraft.combatdepot.common.inventory.menu.GunViewMenu;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record LoadPageMessage(int pageIndex, List<ItemStack> items) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(pageIndex);
        buf.writeCollection(items, FriendlyByteBuf::writeItem);
    }

    public static LoadPageMessage decode(FriendlyByteBuf buf) {
        int pageIndex = buf.readVarInt();
        List<ItemStack> items = buf.readCollection(ArrayList::new, FriendlyByteBuf::readItem);
        return new LoadPageMessage(pageIndex, items);
    }

    public static void handle(LoadPageMessage message, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null && player.containerMenu instanceof GunViewMenu menu) {
                menu.loadPage(message.pageIndex(), message.items());
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
