package top.bluecraft.combatdepot.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import top.bluecraft.combatdepot.api.ICard;
import top.bluecraft.combatdepot.common.data.GlobalCardStorage;
import top.bluecraft.combatdepot.common.inventory.menu.GunViewMenu;

import java.util.function.Supplier;

public class AddItemToCardMessage {
    private final String cardName;
    private final int slot;
    private final ItemStack stack;

    public AddItemToCardMessage(String cardName, int slot, ItemStack stack) {
        this.cardName = cardName;
        this.slot = slot;
        this.stack = stack.copy();
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

    public static void handle(AddItemToCardMessage message, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player != null && player.containerMenu instanceof GunViewMenu menu) {
                // 更新Card的实际inventory
                ICard selectedCard = menu.getCards().stream()
                        .filter(c -> c.getName().equals(message.cardName))
                        .findFirst()
                        .orElse(null);

                if (selectedCard != null) {
                    selectedCard.getInventory().set(message.slot, message.stack);

                    // 保存到GlobalCardStorage
                    GlobalCardStorage storage = GlobalCardStorage.get(player.serverLevel());
                    storage.updateInventory(message.cardName, selectedCard.getInventory());
                    storage.setDirty();
                }

                // 刷新显示
                menu.syncDisplayInventory();
            }
        });
        ctx.setPacketHandled(true);
    }
}
