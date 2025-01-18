package top.bluecraft.viewlauncher.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import top.bluecraft.viewlauncher.CombatDepot;
import top.bluecraft.viewlauncher.client.menu.GunViewMenu;
import top.bluecraft.viewlauncher.common.card.Card;
import top.bluecraft.viewlauncher.common.page.Page;

import java.util.List;
import java.util.function.Supplier;

public class CardSelectionMessage {
    private final int selectedIndex;
    private final int x, y, z;

    public CardSelectionMessage(int selectedIndex, int x, int y, int z) {
        this.selectedIndex = selectedIndex;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static void encode(CardSelectionMessage message, FriendlyByteBuf buffer) {
        buffer.writeInt(message.selectedIndex);
        buffer.writeInt(message.x);
        buffer.writeInt(message.y);
        buffer.writeInt(message.z);
    }

    public static CardSelectionMessage decode(FriendlyByteBuf buffer) {
        return new CardSelectionMessage(
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt()
        );
    }

    public static class Handler {
        public static void handle(CardSelectionMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                // 确保我们在服务器端
                if (!context.getDirection().getReceptionSide().isServer()) return;

                ServerPlayer player = context.getSender();
                if (player == null) return;

                Level world = player.level();
                BlockPos pos = new BlockPos(message.x, message.y, message.z);

                // 确保区块已加载

                if (!world.isLoaded(pos)) return;

                // 获取玩家当前打开的容器
                AbstractContainerMenu container = player.containerMenu;
                if (!(container instanceof GunViewMenu menu)) return;

                // 验证选择的卡片索引
                if (message.selectedIndex < 0 || message.selectedIndex >= menu.cards.size()) {
                    CombatDepot.LOGGER.warn("Invalid card index received: {}", message.selectedIndex);
                    return;
                }

                // 更新服务器端的选择
                menu.selectedCardIndex = message.selectedIndex;
                menu.currentCard = menu.cards.get(message.selectedIndex);

                // 重置页面索引到第一页
                menu.currentPageIndex = 0;
                if (menu.currentCard != null) {
                    menu.currentCard.switchToPage(0);


                    Page newPage = menu.currentCard.getPage(0);
                    List<ItemStack> items = newPage.items();

                    // 更新显示的物品
                    for (int i = 0; i < Math.min(items.size(), Card.ITEMS_PER_PAGE); i++) {
                        int slotIndex = i + 37; // 37是起始槽位索引
                        if (slotIndex >= menu.slots.size()) break;

                        ItemStack stack = items.get(i);
                        menu.slots.get(slotIndex).set(stack);
                    }
                }


                // 广播更改给所有客户端
                menu.broadcastChanges();

                syncToNearbyPlayers(world, pos, message);
            });
            context.setPacketHandled(true);
        }
    }
    private static void syncToNearbyPlayers(Level world, BlockPos pos, CardSelectionMessage message) {
        if (!(world instanceof ServerLevel)) return;

        for (Player otherPlayer : world.players()) {
            if (otherPlayer instanceof ServerPlayer serverPlayer &&
                    serverPlayer.containerMenu instanceof GunViewMenu &&
                    isPlayerNearby(serverPlayer, pos, 8)) {  // 8格范围内

                CombatDepot.PACKET_HANDLER.send(
                        PacketDistributor.PLAYER.with(() -> serverPlayer),
                        new ClientboundCardSelectionPacket(message)
                );
            }
        }
    }

    private static boolean isPlayerNearby(Player player, BlockPos pos, int range) {
        return player.distanceToSqr(
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5
        ) <= range * range;
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }
    public int getX() {
        return x;
    }
    public int getY() {
        return y;
    }
    public int getZ() {
        return z;
    }
}

