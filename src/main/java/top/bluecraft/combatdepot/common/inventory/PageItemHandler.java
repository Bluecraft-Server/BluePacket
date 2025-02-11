package top.bluecraft.combatdepot.common.inventory;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import top.bluecraft.combatdepot.api.ICard;

public class PageItemHandler extends ItemStackHandler {
    private final ICard card;
    private final int page;
    private final int slotsPerPage;
    private final int pageStartIndex;
    private boolean isLoading = false;
    private int handlerId; // 唯一标识符

    public PageItemHandler(ICard card, int page, int slotsPerPage, int handlerId) {
        super(slotsPerPage);
        this.card = card;
        this.page = page;
        this.slotsPerPage = slotsPerPage;
        this.pageStartIndex = page * slotsPerPage;
        this.handlerId = handlerId;
    }

    @Override
    public void setStackInSlot(int slot, @NotNull ItemStack stack) {
        validateSlotIndex(slot);
        if (isLoading) {
            stacks.set(slot, stack.copy());
            return;
        }

        ItemStack existing = stacks.get(slot);
        if (!ItemStack.matches(existing, stack)) {
            stacks.set(slot, stack.copy());
            onContentsChanged(slot);
        }
    }

    @Override
    @NotNull
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        validateSlotIndex(slot);

        ItemStack existing = stacks.get(slot);
        if (existing.isEmpty()) return ItemStack.EMPTY;

        int toExtract = Math.min(amount, existing.getMaxStackSize());
        if (toExtract > existing.getCount()) {
            toExtract = existing.getCount();
        }

        if (simulate) {
            ItemStack copy = existing.copy();
            copy.setCount(toExtract);
            return copy;
        }

        ItemStack extracted = existing.copy();
        extracted.setCount(toExtract);
        existing.shrink(toExtract);

        if (existing.isEmpty()) {
            stacks.set(slot, ItemStack.EMPTY);
        }

        onContentsChanged(slot);
        return extracted;
    }

    @Override
    protected void onContentsChanged(int slot) {
        if (isLoading) return;

        int globalIndex = pageStartIndex + slot;
        if (globalIndex < card.getInventory().size()) {
            card.getInventory().set(globalIndex, getStackInSlot(slot).copy());
        }
    }

    public void loadFromCard() {
        isLoading = true;
        try {
            NonNullList<ItemStack> inventory = card.getInventory();
            for (int i = 0; i < slotsPerPage; i++) {
                int globalIndex = pageStartIndex + i;
                if (globalIndex < inventory.size()) {
                    setStackInSlot(i, inventory.get(globalIndex).copy());
                } else {
                    setStackInSlot(i, ItemStack.EMPTY);
                }
            }
        } finally {
            isLoading = false;
        }
    }

    public void saveToCard() {
        NonNullList<ItemStack> inventory = card.getInventory();
        for (int i = 0; i < slotsPerPage; i++) {
            int globalIndex = pageStartIndex + i;
            if (globalIndex < inventory.size()) {
                inventory.set(globalIndex, getStackInSlot(i).copy());
            }
        }
    }

    public int getHandlerId() {
        return handlerId;
    }

    public int getPage() {
        return page;
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return true;
    }
}
