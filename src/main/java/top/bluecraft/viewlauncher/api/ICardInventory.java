package top.bluecraft.viewlauncher.api;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;

public interface ICardInventory extends IItemHandler, INBTSerializable<CompoundTag>{
    void setStackInSlot(int slot, ItemStack stack);
    @NotNull ItemStack getStackInSlot(int slot);
    int getSlots();
    void clear();
    String getName();
}
