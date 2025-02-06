package top.bluecraft.combatdepot.common.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jetbrains.annotations.NotNull;
import top.bluecraft.combatdepot.common.inventory.menu.CombatDepotMenu;

public class TerminalProvider implements MenuProvider {

    public TerminalProvider() {
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.literal("General Terminal");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, @NotNull Inventory inventory, @NotNull Player player) {
        return new CombatDepotMenu(id, inventory);
    }
}
