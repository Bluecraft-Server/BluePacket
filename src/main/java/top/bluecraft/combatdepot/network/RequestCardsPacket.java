package top.bluecraft.combatdepot.network;

import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import top.bluecraft.combatdepot.CombatDepot;
import top.bluecraft.combatdepot.common.data.GlobalCardStorage;
import top.bluecraft.combatdepot.common.inventory.menu.GunViewMenu;
import top.bluecraft.combatdepot.config.CardConfig;

import java.util.List;
import java.util.function.Supplier;

public class RequestCardsPacket {
    public RequestCardsPacket() {
    }

    public RequestCardsPacket(FriendlyByteBuf buf) {
    }

    public void encode(FriendlyByteBuf buf) {
    }

    public static RequestCardsPacket decode(FriendlyByteBuf buf) {
        return new RequestCardsPacket();
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null && player.containerMenu instanceof GunViewMenu menu) {
                // 获取服务端数据
                GlobalCardStorage storage = GlobalCardStorage.get(player.serverLevel());
                CardConfig config = CardConfig.load();

                System.out.println("发送请求");
                config.loadFromGlobalStorage(storage);
                List<GunViewMenu.Card> cards = menu.getCards();

                for (GunViewMenu.Card card : menu.getCards()) {
                    NonNullList<ItemStack> savedInventory = storage.getInventory(card.getName());
                    if (savedInventory != null) {
                        // 将全局存储的数据复制到卡片库存
                        NonNullList<ItemStack> inventory = card.getInventory();
                        for (int i = 0; i < Math.min(savedInventory.size(), inventory.size()); i++) {
                            inventory.set(i, savedInventory.get(i).copy());
                        }
                    }
                }

                // 发送数据回客户端
                CombatDepot.PACKET_HANDLER.send(
                        PacketDistributor.PLAYER.with(() -> player),
                        new SyncCardsPacket(cards, 0)
                );
            }
        });
        ctx.get().setPacketHandled(true);
    }
}