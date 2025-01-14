package top.bluecraft.bluepacket.common.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import top.bluecraft.bluepacket.client.menu.GunViewMenu;

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

        // 如果物品有保存的数据，加载它
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("MenuData")) {
            CompoundTag menuTag = tag.getCompound("MenuData");
            if (menuTag.contains("Inventories")) {
                menu.loadInventories(menuTag.getCompound("Inventories"));
            }
            if (tag.contains("MenuState")) {
                menu.loadState(tag.getCompound("MenuState"));
            }
        }

        return menu;
    }
}
