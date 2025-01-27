package top.bluecraft.viewlauncher.common.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import top.bluecraft.viewlauncher.CombatDepot;
import top.bluecraft.viewlauncher.api.ICardInventory;
import top.bluecraft.viewlauncher.client.menu.GunViewMenu;

public class CardInventoryCapability implements ICardInventory, IItemHandlerModifiable {
    private final ItemStackHandler inventory;
    private final LazyOptional<ICardInventory> holder;
    private final String name;

    public CardInventoryCapability(int size, String name) {
        this.inventory = new ItemStackHandler(size) {
            @Override
            protected void onContentsChanged(int slot) {
                super.onContentsChanged(slot);
                GunViewMenu.saveInventories();
            }
        };
        this.holder = LazyOptional.of(() -> this);
        this.name = name;
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

    public CompoundTag serializeNBT() {
        return inventory.serializeNBT();
    }

    public void deserializeNBT(CompoundTag nbt) {
        inventory.deserializeNBT(nbt);
    }


    @Override
    public void clear() {
        for (int i = 0; i < inventory.getSlots(); i++) {
            inventory.setStackInSlot(i, ItemStack.EMPTY);
        }
    }

    @Override
    public String getName() {
        return name;
    }

    public LazyOptional<ICardInventory> getHolder() {
        return holder;
    }
}