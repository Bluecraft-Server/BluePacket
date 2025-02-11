package top.bluecraft.combatdepot.common.inventory;

import net.minecraft.world.item.ItemStack;
import top.bluecraft.combatdepot.api.ICard;

public class SlotState {
    private final String cardName;
    private final int pageIndex;
    private final int slotIndex;
    private ItemStack cachedStack;
    private final long stateId;

    public SlotState(String cardName, int pageIndex, int slotIndex, long stateId) {
        this.cardName = cardName;
        this.pageIndex = pageIndex;
        this.slotIndex = slotIndex;
        this.stateId = stateId;
        this.cachedStack = ItemStack.EMPTY;
    }

    public boolean matchesCurrentState(ICard card, int currentPage) {
        return card != null &&
                card.getName().equals(cardName) &&
                pageIndex == currentPage;
    }

    public ItemStack getCachedStack() {
        return cachedStack;
    }

    public void setCachedStack(ItemStack stack) {
        this.cachedStack = stack.copy();
    }

    public long getStateId() {
        return stateId;
    }

    public int getSlotIndex() {
        return slotIndex;
    }

    public boolean isValid(String currentCard, int currentPage, long currentStateId) {
        return cardName.equals(currentCard) &&
                pageIndex == currentPage &&
                stateId == currentStateId;
    }
}
