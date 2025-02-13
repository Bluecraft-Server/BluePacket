package top.bluecraft.combatdepot.common.item;

import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

public class DistributionTerminalProvider implements MenuProvider {
    @Override
    public @NotNull net.minecraft.network.chat.Component getDisplayName() {
        return net.minecraft.network.chat.Component.literal("Distribution Terminal");
    }

    @Override
    public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id, @NotNull net.minecraft.world.entity.player.Inventory inventory, @NotNull Player player) {
        return new top.bluecraft.combatdepot.common.inventory.menu.DistributionMenu(id, inventory, 0, 0);
    }
}