package top.bluecraft.viewlauncher.common.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import top.bluecraft.viewlauncher.ViewLauncher;
import top.bluecraft.viewlauncher.api.ICardInventory;
import top.bluecraft.viewlauncher.client.menu.GunViewMenu;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CardInventoryCapability implements ICardInventory {
    private final ItemStackHandler inventory;
    private final LazyOptional<ICardInventory> holder;

    public CardInventoryCapability(int size, String name) {
        this.inventory = new ItemStackHandler(size) {
            @Override
            protected void onContentsChanged(int slot) {
                super.onContentsChanged(slot);
                GunViewMenu.saveInventories();
            }
        };
        this.holder = LazyOptional.of(() -> this);
        GunViewMenu.cardInventories.put(name, this);
    }

    @Override
    public void setStackInSlot(int slot, @NotNull ItemStack stack) {
        inventory.setStackInSlot(slot, stack);
    }

    @Override
    public @NotNull ItemStack getStackInSlot(int slot) {
        return inventory.getStackInSlot(slot);
    }

    @Override
    public int getSlots() {
        return inventory.getSlots();
    }

    @Override
    public int getSlotLimit(int slot) {
        return inventory.getSlotLimit(slot);
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return inventory.isItemValid(slot, stack);
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        return inventory.insertItem(slot, stack, simulate);
    }

    @Override
    public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
        return inventory.extractItem(slot, amount, simulate);
    }

    @Override
    public CompoundTag serializeNBT() {
        return inventory.serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        inventory.deserializeNBT(nbt);
    }


    @Override
    public void clear() {
        for (int i = 0; i < inventory.getSlots(); i++) {
            inventory.setStackInSlot(i, ItemStack.EMPTY);
        }
    }

    public LazyOptional<ICardInventory> getHolder() {
        return holder;
    }
}