package top.bluecraft.combatdepot.api;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

public interface ICardInventory extends INBTSerializable<CompoundTag>{
    void setStackInSlot(int slot, ItemStack stack);
    @NotNull ItemStack getStackInSlot(int slot);
    int getSlots();
    void clear();
    String getName();
    ItemStackHandler getInventory();
}
