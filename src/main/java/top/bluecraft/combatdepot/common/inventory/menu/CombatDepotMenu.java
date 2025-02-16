package top.bluecraft.combatdepot.common.inventory.menu;

import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import top.bluecraft.combatdepot.CombatDepot;
import top.bluecraft.combatdepot.api.ICard;
import top.bluecraft.combatdepot.common.data.GlobalCardStorage;
import top.bluecraft.combatdepot.config.CardConfig;
import top.bluecraft.combatdepot.init.MenuRegistration;
import top.bluecraft.combatdepot.network.SyncCardsPacket;

import java.util.ArrayList;
import java.util.List;

import static top.bluecraft.combatdepot.config.CardConfig.createDefaultConfig;

public class CombatDepotMenu extends AbstractContainerMenu {

    protected final List<CardSlot> cardSlots = new ArrayList<>();
    private final Player player;
    private final Level world;
    private final List<ICard> cards;
    private int selectedCardIndex;
    private int currentPage;
    private int cardOffset = 0;


    public CombatDepotMenu(int windowId, Inventory playerInventory, int selectedCard, int page) {
        super(MenuRegistration.COMBAT_DEPOT_MENU.get(), windowId);
        this.player = playerInventory.player;
        this.world = player.level();
        this.selectedCardIndex = selectedCard;
        this.currentPage = page;

        // 加载卡片配置
        CardConfig config = CardConfig.load();
        if (config == null) {
            config = createDefaultConfig();
        }

        // 确保在服务端时从SavedData加载最新数据
        if (!world.isClientSide()) {
            GlobalCardStorage storage = GlobalCardStorage.get((ServerLevel) world);
            config.loadFromGlobalStorage(storage);
        }

        this.cards = config.createCards();

        // 初始化槽位
        initializeSlots(playerInventory);
        updateCardSlots();
    }

    public static int getSlotSize() {
        return MenuConstants.SLOTS_PER_PAGE;
    }

    protected void initializeSlots(Inventory playerInventory) {
        // 添加卡片槽位
        for (int row = 0; row < 10; row++) {
            for (int col = 0; col < 11; col++) {
                int index = col + row * 11;
                int x = 10 + col * MenuConstants.SLOT_SIZE;
                int y = 10 + row * MenuConstants.SLOT_SIZE;
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
                        MenuConstants.PLAYER_INVENTORY_START_X + col * MenuConstants.SLOT_SIZE,
                        MenuConstants.PLAYER_INVENTORY_START_Y + row * MenuConstants.SLOT_SIZE));
            }
        }

        // 添加玩家快捷栏槽位
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory,
                    col,
                    MenuConstants.PLAYER_INVENTORY_START_X + col * MenuConstants.SLOT_SIZE,
                    MenuConstants.HOTBAR_START_Y));
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
        int startIndex = currentPage * MenuConstants.SLOTS_PER_PAGE;

        // 更新所有卡片槽位
        for (CardSlot slot : cardSlots) {
            int globalIndex = startIndex + slot.getSlotIndex();
            if (globalIndex < inventory.size()) {
                slot.updateDisplayedItem(inventory.get(globalIndex));
            } else {
                slot.updateDisplayedItem(ItemStack.EMPTY);
            }
        }

        // 在客户端时发送同步包到服务端
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

            if (index < MenuConstants.PLAYER_INVENTORY_START) {
                // Move from card inventory to player inventory
                if (!this.moveItemStackTo(stack, MenuConstants.PLAYER_INVENTORY_START, MenuConstants.PLAYER_INVENTORY_END, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Move from player inventory to card inventory
                if (!this.moveItemStackTo(stack, MenuConstants.CARD_INVENTORY_START, MenuConstants.PLAYER_INVENTORY_START, false)) {
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

    public void setCards(List<ICard> newCards) {
        cards.clear();
        cards.addAll(newCards);
        if (selectedCardIndex >= cards.size()) {
            selectedCardIndex = Math.max(0, cards.size() - 1);
        }
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

    public void syncDisplayInventory() {
        if (!world.isClientSide) {
            ServerPlayer serverPlayer = (ServerPlayer) player;
            CombatDepot.PACKET_HANDLER.send(
                    PacketDistributor.PLAYER.with(() -> serverPlayer),
                    new SyncCardsPacket(cards, cardOffset)
            );
        }
    }

    protected static class CardSlot extends Slot {
        protected final CombatDepotMenu menu;
        protected final int slotIndex;
        protected ItemStack displayedItem = ItemStack.EMPTY;

        public CardSlot(CombatDepotMenu menu, int index, int x, int y) {
            super(new Inventory(menu.player) {
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
            }
        }

        @Override
        public void setChanged() {
            super.setChanged();
            // 移除保存调用，只保留客户端同步
            if (menu.getWorld().isClientSide()) {
                menu.syncDisplayInventory();
            }
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

        public int getGlobalIndex() {
            return menu.getCurrentPage() * MenuConstants.SLOTS_PER_PAGE + slotIndex;
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