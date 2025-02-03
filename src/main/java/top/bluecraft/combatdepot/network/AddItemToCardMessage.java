package top.bluecraft.combatdepot.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import top.bluecraft.combatdepot.api.ICardInventory;
import top.bluecraft.combatdepot.common.data.GlobalCardStorage;
import top.bluecraft.combatdepot.common.inventory.menu.GunViewMenu;

import java.util.List;
import java.util.function.Supplier;

public class AddItemToCardMessage {
    private final String cardName;
    private final int slot;
    private final ItemStack stack;

    public AddItemToCardMessage(String cardName, int slot, ItemStack stack) {
        this.cardName = cardName;
        this.slot = slot;
        this.stack = stack;
    }

    public static void encode(AddItemToCardMessage message, FriendlyByteBuf buf) {
        buf.writeUtf(message.cardName);
        buf.writeInt(message.slot);
        buf.writeItem(message.stack);
    }

    public static AddItemToCardMessage decode(FriendlyByteBuf buffer) {
        return new AddItemToCardMessage(
                buffer.readUtf(),
                buffer.readInt(),
                buffer.readItem()
        );
    }

    public static void handle(AddItemToCardMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null && player.hasPermissions(2) &&
                    player.containerMenu instanceof GunViewMenu menu) {
                GlobalCardStorage dataStorage = GlobalCardStorage.get(player.serverLevel());
                dataStorage.updatePlayerInventory(player, message.cardName, menu.getDisplayHandler());
            }
        });
        context.setPacketHandled(true);
    }
}
