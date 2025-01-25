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

public record CardSelectionMessage(int selectedIndex, int x, int y, int z) {

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
        public static void handle(CardSelectionMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                // Validate server-side processing
                if (!context.getDirection().getReceptionSide().isServer()) return;

                ServerPlayer player = context.getSender();
                if (player == null) return;

                Level world = player.level();
                BlockPos pos = new BlockPos(message.x, message.y, message.z);

                // Validate world and chunk loading
                if (world == null || !world.isLoaded(pos)) return;

                // Validate current container
                AbstractContainerMenu container = player.containerMenu;
                if (!(container instanceof GunViewMenu menu)) return;

                // Validate card selection
                if (message.selectedIndex < 0 || message.selectedIndex >= menu.cards.size()) {
                    CombatDepot.LOGGER.warn("Invalid card index received: {}", message.selectedIndex);
                    return;
                }

                // Update server-side selection
                try {
                    menu.selectedCardIndex = message.selectedIndex;
                    menu.currentCard = menu.cards.get(message.selectedIndex);

                    // Reset page index
                    menu.currentPageIndex = 0;

                    if (menu.currentCard != null) {
                        menu.currentCard.switchToPage(0);
                        Page newPage = menu.currentCard.getPage(0);

                        if (newPage != null && newPage.items() != null) {
                            List<ItemStack> items = newPage.items();
                            updateMenuSlots(menu, items);
                        }
                    }

                    // Broadcast changes
                    menu.broadcastChanges();

                    // Sync to nearby players
                    syncToNearbyPlayers(world, pos, message);
                } catch (Exception e) {
                    CombatDepot.LOGGER.error("Error processing card selection", e);
                }
            });
            context.setPacketHandled(true);
        }

        private static void updateMenuSlots(GunViewMenu menu, List<ItemStack> items) {
            for (int i = 0; i < Math.min(items.size(), Card.ITEMS_PER_PAGE); i++) {
                int slotIndex = i + 37; // Starting slot index
                if (slotIndex >= menu.slots.size()) break;

                ItemStack stack = items.get(i);
                menu.slots.get(slotIndex).set(stack != null ? stack : ItemStack.EMPTY);
            }
        }

    private static void syncToNearbyPlayers(Level world, BlockPos pos, CardSelectionMessage message) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        for (Player otherPlayer : serverLevel.players()) {
            if (otherPlayer instanceof ServerPlayer serverPlayer
                    && serverPlayer.containerMenu instanceof GunViewMenu
                    && isPlayerNearby(serverPlayer, pos, 8)) {

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
}