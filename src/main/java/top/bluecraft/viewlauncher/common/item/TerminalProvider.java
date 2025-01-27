package top.bluecraft.viewlauncher.common.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import top.bluecraft.viewlauncher.client.menu.GunViewMenu;

public class TerminalProvider implements MenuProvider {
    private final ItemStack stack;

    public TerminalProvider(ItemStack stack) {
        this.stack = stack;
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.literal("General Terminal");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, @NotNull Inventory inventory, @NotNull Player player) {
        GunViewMenu menu = new GunViewMenu(id, inventory);

        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("MenuData")) {
            CompoundTag menuTag = tag.getCompound("MenuData");

            // Load inventories with a null check
            if (menuTag.contains("Inventories")) {
                CompoundTag inventoriesTag = menuTag.getCompound("Inventories");
                if (!inventoriesTag.isEmpty()) {
                    GunViewMenu.loadInventories(inventoriesTag);
                }
            }

            // Load menu state with a null check
            if (tag.contains("MenuState")) {
                CompoundTag stateTag = tag.getCompound("MenuState");
                if (!stateTag.isEmpty()) {
                    menu.loadState(stateTag);
                }
            }
        }

        return menu;
    }
}
