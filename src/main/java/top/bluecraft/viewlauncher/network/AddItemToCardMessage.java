package top.bluecraft.viewlauncher.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import top.bluecraft.viewlauncher.api.ICard;
import top.bluecraft.viewlauncher.api.ICardInventory;
import top.bluecraft.viewlauncher.client.menu.GunViewMenu;

import java.util.function.Supplier;

public class AddItemToCardMessage {
    private final int cardIndex;
    private final int slot;
    private final ItemStack stack;

    public AddItemToCardMessage(int cardIndex, int slot, ItemStack stack) {
        this.cardIndex = cardIndex;
        this.slot = slot;
        this.stack = stack;
    }

    public static void encode(AddItemToCardMessage message, FriendlyByteBuf buf) {
        buf.writeInt(message.cardIndex);
        buf.writeInt(message.slot);
        buf.writeItem(message.stack);
    }

    public static AddItemToCardMessage decode(FriendlyByteBuf buffer) {
        return new AddItemToCardMessage(
                buffer.readInt(),
                buffer.readInt(),
                buffer.readItem()
        );
    }

    public static void handle(AddItemToCardMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            // 检查权限
            if (!player.hasPermissions(2)) {
                player.sendSystemMessage(Component.translatable("gui.bluepacket.permission"));
                return;
            }

            // 获取玩家当前打开的容器
            if (player.containerMenu instanceof GunViewMenu menu) {
                // 通过索引获取卡片
                if (message.cardIndex >= 0 && message.cardIndex < menu.cards.size()) {
                    ICard targetCard = menu.cards.get(message.cardIndex);
                    ICardInventory inventory = targetCard.getInventory();

                    // 更新物品栏
                    inventory.setStackInSlot(message.slot, message.stack);

                    // 广播更改
                    menu.broadcastChanges();
                }
            }
        });
        context.setPacketHandled(true);
    }
}
