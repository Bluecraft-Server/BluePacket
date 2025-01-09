package top.bluecraft.bluepacket.client.menu.slot;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import top.bluecraft.bluepacket.BluePacket;
import top.bluecraft.bluepacket.network.GunViewSlotMessage;

public class WeaponViewSlot extends Slot {
    private final int slotID;
    private final Level world;
    private final Player player;
    public final int px, py, pz;

    public WeaponViewSlot(Container inv, int slotID, int x, int y, Level world, Player player, ItemStack itemStack, int px, int py, int pz) {
        super(inv, slotID, x, y);
        this.slotID = slotID;
        this.world = world;
        this.player = player;
        this.px = px;
        this.py = py;
        this.pz = pz;
        this.set(itemStack);
    }

    @Override
    public boolean mayPickup(@NotNull Player pPlayer) {
        return false;
    }

    @Override
    public boolean mayPlace(@NotNull ItemStack pStack) {
        return false;
    }

    @Override
    public void onTake(@NotNull Player pPlayer, @NotNull ItemStack pStack) {
        super.onTake(pPlayer, pStack);
        slotChanged(slotID, 1, 0);
    }

    private void slotChanged(int slotid, int ctype, int meta) {
        if (this.world != null && this.world.isClientSide()) {
            BluePacket.PACKET_HANDLER.sendToServer(new GunViewSlotMessage(slotid, px, py, pz, ctype, meta));
            GunViewSlotMessage.handleSlotAction(player, slotid, ctype, meta, px, py, pz);
        }
    }
}
