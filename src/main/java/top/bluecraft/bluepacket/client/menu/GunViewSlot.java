package top.bluecraft.bluepacket.client.menu;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;
import top.bluecraft.bluepacket.BluePacket;
import top.bluecraft.bluepacket.network.GunViewChangeMessage;

public class GunViewSlot extends SlotItemHandler {
    private final GunViewMenu menu;

    public GunViewSlot(IItemHandler itemHandler, int index, int x, int y, GunViewMenu menu) {
        super(itemHandler, index, x, y);
        this.menu = menu;
    }

    @Override
    public boolean mayPlace(@NotNull ItemStack stack) {
        return false;
    }

    @Override
    public boolean mayPickup(@NotNull Player player) {
        return true;
    }

    @Override
    public void onTake(@NotNull Player player, @NotNull ItemStack stack) {
        super.onTake(player, stack);
        if (menu.world.isClientSide() && menu.currentCard != null) {
            BluePacket.PACKET_HANDLER.sendToServer(
                    new GunViewChangeMessage(menu.currentPageIndex,
                            menu.currentCard.getTotalPages(),
                            getSlotIndex(),
                            menu.x, menu.y, menu.z,
                            1, 0)
            );
        }
    }
}