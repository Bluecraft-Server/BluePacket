package top.bluecraft.combatdepot.common.inventory.menu;

import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import top.bluecraft.combatdepot.CombatDepot;
import top.bluecraft.combatdepot.api.ICard;
import top.bluecraft.combatdepot.common.data.GlobalCardStorage;
import top.bluecraft.combatdepot.config.CardConfig;
import top.bluecraft.combatdepot.init.MenuRegistration;
import top.bluecraft.combatdepot.network.SyncCardsPacket;

import java.util.ArrayList;
import java.util.List;

public class CombatDepotMenu extends AbstractContainerMenu {
    private static final int PLAYER_INVENTORY_START_X = 220;
    private static final int PLAYER_INVENTORY_START_Y = 111;
    private static final int HOTBAR_START_Y = 169;
    private static final int SLOT_SIZE = 18;
    private static final int SLOTS_PER_PAGE = 110;

    private final Player player;
    private final Level world;
    private final List<ICard> cards;
    private int selectedCardIndex = 0;
    private int currentPage = 0;
    private int cardOffset = 0;
    private final List<CardSlot> cardSlots = new ArrayList<>();

    // Slot ranges
    private static final int CARD_INVENTORY_START = 0;
    private static final int PLAYER_INVENTORY_START = SLOTS_PER_PAGE;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 36;

    public CombatDepotMenu(int windowId, Inventory playerInventory, int selectedCard, int page) {
        super(MenuRegistration.GUN_VIEW_MENU.get(), windowId);
        this.player = playerInventory.player;
        this.world = player.level();
        this.selectedCardIndex = selectedCard;
        this.currentPage = page;

        // 加载卡片配置
        CardConfig config = CardConfig.load();
        if (!world.isClientSide()) {
            GlobalCardStorage storage = GlobalCardStorage.get((ServerLevel) world);
            config.loadFromGlobalStorage(storage);
        }
        this.cards = config.createCards();

        // 初始化槽位
        initializeSlots(playerInventory);

        // 立即更新显示
        updateCardSlots();
    }

    private void initializeSlots(Inventory playerInventory) {
        // 添加卡片槽位
        for (int row = 0; row < 10; row++) {
            for (int col = 0; col < 11; col++) {
                int index = col + row * 11;
                int x = 10 + col * SLOT_SIZE;
                int y = 10 + row * SLOT_SIZE;
                CardSlot slot = new CardSlot(this, index, x, y);
                cardSlots.add(slot);
                addSlot(slot);
            }
        }

        // 添加玩家物品栏槽位
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory,
                        col + (row + 1) * 9,
                        PLAYER_INVENTORY_START_X + col * SLOT_SIZE,
                        PLAYER_INVENTORY_START_Y + row * SLOT_SIZE));
            }
        }

        // 添加玩家快捷栏槽位
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory,
                    col,
                    PLAYER_INVENTORY_START_X + col * SLOT_SIZE,
                    HOTBAR_START_Y));
        }
    }

    public void setPage(int page) {
        if (getCurrentCard() != null && page >= 0 && page < getCurrentCard().getTotalPages()) {
            currentPage = page;
            updateCardSlots();

            // 如果在服务端，同步到客户端
            if (!world.isClientSide()) {
                syncDisplayInventory();
            }
        }
    }

    public void selectCard(int index) {
        if (index >= 0 && index < cards.size() && index != selectedCardIndex) {
            selectedCardIndex = index;
            currentPage = 0;
            updateCardSlots();

            // 如果在服务端，同步到客户端
            if (!world.isClientSide()) {
                syncDisplayInventory();
            }
        }
    }

    private void updateCardSlots() {
        ICard currentCard = getCurrentCard();
        if (currentCard == null) return;

        NonNullList<ItemStack> inventory = currentCard.getInventory();
        int startIndex = currentPage * SLOTS_PER_PAGE;

        // 更新所有卡片槽位
        for (CardSlot slot : cardSlots) {
            int globalIndex = startIndex + slot.getSlotIndex();
            if (globalIndex < inventory.size()) {
                slot.updateDisplayedItem(inventory.get(globalIndex));
            } else {
                slot.updateDisplayedItem(ItemStack.EMPTY);
            }
        }

        // 强制同步到客户端
        if (!world.isClientSide()) {
            syncDisplayInventory();
        }
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            itemstack = stack.copy();

            if (index < PLAYER_INVENTORY_START) {
                // Move from card inventory to player inventory
                if (!this.moveItemStackTo(stack, PLAYER_INVENTORY_START, PLAYER_INVENTORY_END, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Move from player inventory to card inventory
                if (!this.moveItemStackTo(stack, CARD_INVENTORY_START, PLAYER_INVENTORY_START, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return itemstack;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return true;
    }

    // Getters and setters
    public ICard getCurrentCard() {
        return selectedCardIndex >= 0 && selectedCardIndex < cards.size() ? cards.get(selectedCardIndex) : null;
    }

    public List<ICard> getCards() {
        return cards;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public int getSelectedCardIndex() {
        return selectedCardIndex;
    }

    public int getCardOffset() {
        return cardOffset;
    }

    public void setCardOffset(int offset) {
        this.cardOffset = offset;
    }

    public Player getPlayer() {
        return player;
    }

    public Level getWorld() {
        return world;
    }

    public static int getSlotSize() {
        return SLOTS_PER_PAGE;
    }

    public void syncDisplayInventory() {
        if (!world.isClientSide()) {
            CombatDepot.PACKET_HANDLER.sendToServer(new SyncCardsPacket(cards, cardOffset));
        }
    }

    public void savePersistentData() {
        if (!world.isClientSide()) {
            GlobalCardStorage storage = GlobalCardStorage.get((ServerLevel) world);
            ICard currentCard = getCurrentCard();
            if (currentCard != null) {
                storage.updateInventory(currentCard.getName(), currentCard.getInventory());
                storage.setDirty();
            }
        }
    }

    public void setCards(List<ICard> newCards) {
        cards.clear();
        cards.addAll(newCards);
        if (selectedCardIndex >= cards.size()) {
            selectedCardIndex = Math.max(0, cards.size() - 1);
        }
    }

    // Inner classes
    public static class Card implements ICard {
        private final CardConfig.CardEntry entry;
        private NonNullList<ItemStack> inventory;

        public Card(CardConfig.CardEntry entry, NonNullList<ItemStack> inventory) {
            this.entry = entry;
            this.inventory = inventory;
        }

        @Override
        public String getName() {
            return entry.getName();
        }

        @Override
        public NonNullList<ItemStack> getInventory() {
            return inventory;
        }

        @Override
        public void setInventory(NonNullList<ItemStack> inventory) {
            this.inventory = inventory;
        }

        @Override
        public List<ItemStack> getPageItems(int page) {
            int startIndex = page * SLOTS_PER_PAGE;
            int endIndex = Math.min(startIndex + SLOTS_PER_PAGE, inventory.size());
            return inventory.subList(startIndex, endIndex);
        }

        @Override
        public int getTotalPages() {
            return (inventory.size() + SLOTS_PER_PAGE - 1) / SLOTS_PER_PAGE;
        }

        @Override
        public int getSlotsPerPage() {
            return SLOTS_PER_PAGE;
        }

        @Override
        public ResourceLocation getTexture() {
            return entry.getTexture();
        }
    }

    private static class CardSlot extends Slot {
        private final CombatDepotMenu menu;
        private final int slotIndex;
        private ItemStack displayedItem = ItemStack.EMPTY;

        public CardSlot(CombatDepotMenu menu, int index, int x, int y) {
            super(new Inventory(null) {
                @Override
                public @NotNull ItemStack getItem(int slot) {
                    return ItemStack.EMPTY;
                }
            }, index, x, y);
            this.menu = menu;
            this.slotIndex = index;

            // 初始化显示的物品
            if (menu.getCurrentCard() != null) {
                int globalIndex = getGlobalIndex();
                if (globalIndex < menu.getCurrentCard().getInventory().size()) {
                    this.displayedItem = menu.getCurrentCard().getInventory().get(globalIndex).copy();
                }
            }
        }

        @Override
        public boolean mayPlace(@NotNull ItemStack stack) {
            ICard currentCard = menu.getCurrentCard();
            return currentCard != null && getGlobalIndex() < currentCard.getInventory().size();
        }

        @Override
        public @NotNull ItemStack getItem() {
            // 使用缓存的displayedItem而不是直接从inventory获取
            return this.displayedItem;
        }

        @Override
        public void set(@NotNull ItemStack stack) {
            ICard currentCard = menu.getCurrentCard();
            if (currentCard == null) return;

            int globalIndex = getGlobalIndex();
            if (globalIndex >= 0 && globalIndex < currentCard.getInventory().size()) {
                ItemStack newStack = stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
                currentCard.getInventory().set(globalIndex, newStack);
                updateDisplayedItem(newStack);
                setChanged();

                // 保存到全局存储
                menu.savePersistentData();
            }
        }

        @Override
        public void setChanged() {
            super.setChanged();
            menu.syncDisplayInventory();
        }

        @Override
        public @NotNull ItemStack remove(int amount) {
            ItemStack currentItem = getItem();
            if (currentItem.isEmpty()) return ItemStack.EMPTY;

            ItemStack splitStack = currentItem.copy();
            int actualAmount = Math.min(amount, currentItem.getCount());
            splitStack.setCount(actualAmount);
            currentItem.shrink(actualAmount);

            set(currentItem); // 这会更新显示和保存
            return splitStack;
        }

        public void updateDisplayedItem(ItemStack stack) {
            this.displayedItem = stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
        }

        private int getGlobalIndex() {
            return menu.getCurrentPage() * CombatDepotMenu.SLOTS_PER_PAGE + slotIndex;
        }

        public int getSlotIndex() {
            return slotIndex;
        }

        @Override
        public boolean mayPickup(@NotNull Player player) {
            return !getItem().isEmpty();
        }
    }
}