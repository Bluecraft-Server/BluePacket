package top.bluecraft.combatdepot.common.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import top.bluecraft.combatdepot.api.ICardInventory;
import top.bluecraft.combatdepot.common.inventory.menu.GunViewMenu;

public class CardInventoryCapability implements ICardInventory{
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

    public ItemStackHandler getInventory() {
        return inventory;
    }
}