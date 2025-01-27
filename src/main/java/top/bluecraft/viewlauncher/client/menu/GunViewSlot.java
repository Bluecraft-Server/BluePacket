package top.bluecraft.viewlauncher.client.menu;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;
import top.bluecraft.viewlauncher.CombatDepot;
import top.bluecraft.viewlauncher.network.SlotTakeMessage;

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
        if (!mayPickup(player)) {
            return;
        }
        super.onTake(player, stack);

        // 标记槽位已被取出
        menu.markSlotTaken(getSlotIndex());

        // 只在客户端发送消息
        if (player.level().isClientSide()) {
            CombatDepot.PACKET_HANDLER.sendToServer(new SlotTakeMessage(getSlotIndex()));
        }
    }
}