package top.bluecraft.combatdepot.common.inventory.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import top.bluecraft.combatdepot.CombatDepot;
import top.bluecraft.combatdepot.api.ICard;
import top.bluecraft.combatdepot.common.data.GlobalCardStorage;
import top.bluecraft.combatdepot.config.CardConfig;
import top.bluecraft.combatdepot.init.MenuRegistration;
import top.bluecraft.combatdepot.network.SyncCardsPacket;
import top.bluecraft.combatdepot.network.UpdateSlotMessage;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

public class CombatDepotMenu extends AbstractContainerMenu {
    // Region: Constants
    private static final int GRID_ROWS = 10;
    private static final int GRID_COLS = 11;
    private static final int SLOT_SIZE = 110; // 10*11 - 最后一个槽位
    private static final int SLOT_SPACING = 18;

    // GUI 布局坐标
    private static final int GRID_START_X = 10;
    private static final int GRID_START_Y = 10;
    private static final int PLAYER_INV_START_X = 220;
    private static final int PLAYER_INV_START_Y = 111;
    private static final int HOTBAR_START_Y = 169;

    // Region: Fields
    private final Player player;
    private final Level world;
    private final int x, y, z;
    private List<ICard> cards;
    private int selectedCardIndex = -1;
    private int currentPage = 0;
    private int cardOffset = 0;

    // 显示用临时库存（当前页面）
    private final ItemStackHandler displayHandler = new ItemStackHandler(SLOT_SIZE);
    private final BitSet takenSlots = new BitSet(SLOT_SIZE);

    public CombatDepotMenu(int id, Inventory playerInventory) {
        super(MenuRegistration.GUN_VIEW_MENU.get(), id);
        this.player = playerInventory.player;
        this.world = player.level();
        BlockPos pos = player.blockPosition();
        this.x = pos.getX();
        this.y = pos.getY();
        this.z = pos.getZ();
        setupSlots(playerInventory);

        // 初始化卡牌数据（客户端和服务端都需要）
        CardConfig config = CardConfig.load();
        this.cards = config.createCards();

        if (player.getServer() != null) {
            GlobalCardStorage storage = GlobalCardStorage.get(player.getServer().overworld());
            System.out.println("发送请求");
            config.loadFromGlobalStorage(storage);

            for (ICard card : cards) {
                NonNullList<ItemStack> savedInventory = storage.getInventory(card.getName());
                if (savedInventory != null) {
                    // 将全局存储的数据复制到卡片库存
                    NonNullList<ItemStack> inventory = card.getInventory();
                    for (int i = 0; i < Math.min(savedInventory.size(), inventory.size()); i++) {
                        inventory.set(i, savedInventory.get(i).copy());
                    }
                }
            }
        }
        if (!cards.isEmpty()){
            selectCard(0);
        }
    }

    public void markSlotTaken(int slotIndex) {
        takenSlots.set(slotIndex);
        broadcastChanges();
    }

    public boolean isSlotTaken(int slotIndex) {
        return takenSlots.get(slotIndex);
    }

    public void selectCard(int index) {
        if (index >= 0 && index < cards.size()) {
            this.selectedCardIndex = index;
            this.currentPage = 0; // 重置为第一页
            refreshDisplayInventory(); // 刷新显示
            broadcastChanges();
        }
    }

    // 在 CombatDepotMenu 类中添加以下方法

    /**
     * 加载指定页面的物品到显示库存
     * @param pageIndex 页码（从0开始）
     * @param items 当前页的物品列表
     */
    public void loadPage(int pageIndex, List<ItemStack> items) {
        // 清空当前显示库存
        for (int i = 0; i < SLOT_SIZE; i++) {
            displayHandler.setStackInSlot(i, ItemStack.EMPTY);
            takenSlots.clear(i);
        }

        // 加载新的页面数据
        for (int i = 0; i < Math.min(items.size(), SLOT_SIZE); i++) {
            ItemStack stack = items.get(i);
            if (!stack.isEmpty()) {
                displayHandler.setStackInSlot(i, stack.copy());
                takenSlots.set(i); // 标记槽位已被取出过
            }
        }

        // 通知客户端同步
        broadcastChanges();

        // 调试日志（可选）
        CombatDepot.LOGGER.debug("Loaded page {} with {} items", pageIndex, items.size());
    }

    /**
     * 获取当前显示库存的页面物品
     */
    public List<ItemStack> getCurrentPageItems() {
        List<ItemStack> items = new ArrayList<>(SLOT_SIZE);
        for (int i = 0; i < SLOT_SIZE; i++) {
            items.add(displayHandler.getStackInSlot(i));
        }
        return items;
    }

    public void setPage(int page) {
        if (selectedCardIndex == -1) return;

        ICard card = cards.get(selectedCardIndex);
        int totalPages = card.getTotalPages();
        if (page >= 0 && page < totalPages) {
            this.currentPage = page;
            refreshDisplayInventory(); // 刷新显示
            broadcastChanges();
        } else {
            CombatDepot.LOGGER.warn("无效页面: {} (总页数: {})", page, totalPages);
        }
    }

    private void refreshDisplayInventory() {
        if (selectedCardIndex == -1) return;

        ICard card = cards.get(selectedCardIndex);
        int startIndex = currentPage * SLOT_SIZE;

        // 清空当前显示
        for (int i = 0; i < SLOT_SIZE; i++) {
            displayHandler.setStackInSlot(i, ItemStack.EMPTY);
        }

        // 从 Card 的 inventory 加载到 displayHandler
        for (int i = 0; i < SLOT_SIZE; i++) {
            int actualIndex = startIndex + i;
            if (actualIndex < card.getInventory().size()) {
                ItemStack stack = card.getInventory().get(actualIndex);
                if (!stack.isEmpty()) {
                    displayHandler.setStackInSlot(i, stack.copy());
                    CombatDepot.LOGGER.debug("Card {} inventory at slot {}: {}",
                            card.getName(),
                            actualIndex,
                            stack.isEmpty() ? "empty" : stack.getItem().getDefaultInstance().getDisplayName()
                    );
                }
            }
        }

        // 强制同步
        broadcastChanges();
    }

    // Region: Slot Management
    private void setupSlots(Inventory playerInventory) {
        // 网格槽位
        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLS; col++) {
                int index = row * GRID_COLS + col;

                addSlot(new DynamicSlot(
                        displayHandler,
                        index,
                        GRID_START_X + col * SLOT_SPACING,
                        GRID_START_Y + row * SLOT_SPACING,
                        this
                ));
            }
        }

        // 玩家物品栏
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                addSlot(new Slot(playerInventory,
                        col + row * 9 + 9,
                        PLAYER_INV_START_X + col * SLOT_SPACING,
                        PLAYER_INV_START_Y + row * SLOT_SPACING
                ));
            }
        }

        // 快捷栏
        for (int col = 0; col < 9; ++col) {
            addSlot(new Slot(playerInventory, col,
                    PLAYER_INV_START_X + col * SLOT_SPACING,
                    HOTBAR_START_Y
            ));
        }
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            if (index < 110) {
                if (!this.moveItemStackTo(itemstack1, 1, this.slots.size(), true))
                    return ItemStack.EMPTY;
                slot.onQuickCraft(itemstack1, itemstack);
            } else if (!this.moveItemStackTo(itemstack1, 0, 1, false)) {
                if (index < 110 + 27) {
                    if (!this.moveItemStackTo(itemstack1, 1 + 27, this.slots.size(), true))
                        return ItemStack.EMPTY;
                } else {
                    if (!this.moveItemStackTo(itemstack1, 1, 1 + 27, false))
                        return ItemStack.EMPTY;
                }
                return ItemStack.EMPTY;
            }
            if (itemstack1.getCount() == 0)
                slot.set(ItemStack.EMPTY);
            else
                slot.setChanged();
            if (itemstack1.getCount() == itemstack.getCount())
                return ItemStack.EMPTY;
            slot.onTake(playerIn, itemstack1);
        }
        return itemstack;
    }

    @Override
    protected boolean moveItemStackTo(@NotNull ItemStack p_38904_, int p_38905_, int p_38906_, boolean p_38907_) {
        boolean flag = false;
        int i = p_38905_;
        if (p_38907_) {
            i = p_38906_ - 1;
        }
        if (p_38904_.isStackable()) {
            while (!p_38904_.isEmpty()) {
                if (p_38907_) {
                    if (i < p_38905_) {
                        break;
                    }
                } else if (i >= p_38906_) {
                    break;
                }
                Slot slot = this.slots.get(i);
                ItemStack itemstack = slot.getItem();
                if (slot.mayPlace(itemstack) && !itemstack.isEmpty() && ItemStack.isSameItemSameTags(p_38904_, itemstack)) {
                    int j = itemstack.getCount() + p_38904_.getCount();
                    int maxSize = Math.min(slot.getMaxStackSize(), p_38904_.getMaxStackSize());
                    if (j <= maxSize) {
                        p_38904_.setCount(0);
                        itemstack.setCount(j);
                        slot.set(itemstack);
                        flag = true;
                    } else if (itemstack.getCount() < maxSize) {
                        p_38904_.shrink(maxSize - itemstack.getCount());
                        itemstack.setCount(maxSize);
                        slot.set(itemstack);
                        flag = true;
                    }
                }
                if (p_38907_) {
                    --i;
                } else {
                    ++i;
                }
            }
        }
        if (!p_38904_.isEmpty()) {
            if (p_38907_) {
                i = p_38906_ - 1;
            } else {
                i = p_38905_;
            }
            while (true) {
                if (p_38907_) {
                    if (i < p_38905_) {
                        break;
                    }
                } else if (i >= p_38906_) {
                    break;
                }
                Slot slot1 = this.slots.get(i);
                ItemStack itemstack1 = slot1.getItem();
                if (itemstack1.isEmpty() && slot1.mayPlace(p_38904_)) {
                    if (p_38904_.getCount() > slot1.getMaxStackSize()) {
                        slot1.setByPlayer(p_38904_.split(slot1.getMaxStackSize()));
                    } else {
                        slot1.setByPlayer(p_38904_.split(p_38904_.getCount()));
                    }
                    slot1.setChanged();
                    flag = true;
                    break;
                }
                if (p_38907_) {
                    --i;
                } else {
                    ++i;
                }
            }
        }
        return flag;
    }

    // Region: Data Persistence
    @Override
    public void removed(@NotNull Player player) {
        super.removed(player);
        if (!player.level().isClientSide()) {
            savePersistentData();
        }
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return true;
    }

    public void savePersistentData() {
        if (player.getServer() == null) return;

        GlobalCardStorage storage = GlobalCardStorage.get(player.getServer().overworld());

        cards.forEach(card -> {
            NonNullList<ItemStack> globalInventory = storage.getInventory(card.getName());
            NonNullList<ItemStack> currentInventory = card.getInventory();

            // 添加数据验证
            if (currentInventory == null || currentInventory.isEmpty()) {
                CombatDepot.LOGGER.warn("Card {} has empty inventory!", card.getName());
                return;
            }

            // 确保globalInventory大小正确
            if (globalInventory.size() != currentInventory.size()) {
                globalInventory = NonNullList.withSize(currentInventory.size(), ItemStack.EMPTY);
            }

            // 只保存非空物品
            for (int i = 0; i < currentInventory.size(); i++) {
                ItemStack stack = currentInventory.get(i);
                if (!stack.isEmpty()) {
                    globalInventory.set(i, stack.copy());
                    CombatDepot.LOGGER.debug("Saved item in slot {}: {}", i, stack.getDisplayName().getString());
                }
            }

            storage.updateInventory(card.getName(), globalInventory);
        });

        storage.setDirty();
    }

    public void syncDisplayInventory() {
        List<ItemStack> pageItems = getCurrentCard().getPageItems(currentPage);
        for (int i = 0; i < SLOT_SIZE; i++) {
            displayHandler.setStackInSlot(i, i < pageItems.size() ? pageItems.get(i) : ItemStack.EMPTY);
        }
        broadcastChanges();
    }

    public void setCardOffset(int offset) {
        this.cardOffset = offset;
        broadcastChanges(); // 通知客户端更新
    }

    public void setCards(List<ICard> cards) {
        this.cards = cards;
    }

    // Region: Getters
    public List<ICard> getCards() {
        return cards;
    }

    public int getSelectedCardIndex() {
        return selectedCardIndex;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public ItemStackHandler getDisplayHandler() {
        return displayHandler;
    }

    public Level getWorld() {
        return world;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public Player getPlayer() {
        return player;
    }

    public BitSet getTakenSlots() {
        return takenSlots;
    }

    public ICard getCurrentCard() {
        if (selectedCardIndex >= 0 && selectedCardIndex < cards.size()) {
            return cards.get(selectedCardIndex);
        }
        return null; // 如果没有选中卡片，返回 null
    }

    public static int getSlotSize() {
        return SLOT_SIZE;
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();

        // 服务端向客户端同步数据
        if (!this.world.isClientSide) {
            ServerPlayer player = (ServerPlayer) this.player;
            CombatDepot.PACKET_HANDLER.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new SyncCardsPacket(this.cards, cardOffset)
            );
        }
    }

    public int getCardOffset() {
        return cardOffset;
    }

    public static class Card implements ICard {
        private final String name;
        private NonNullList<ItemStack> inventory;
        private final int slotsPerPage;
        private final ResourceLocation texture;

        public Card(CardConfig.CardEntry entry, NonNullList<ItemStack> inventory) {
            this.name = entry.getName();
            this.inventory = inventory;
            this.slotsPerPage = SLOT_SIZE;
            this.texture = entry.getTexture();
        }

        public void setInventory(NonNullList<ItemStack> inventory) {
            if (inventory == null) {
                CombatDepot.LOGGER.error("Attempted to set null inventory for card: {}", name);
                return;
            }
            this.inventory = inventory;
        }

        public List<ItemStack> getPageItems(int page) {
            int start = page * slotsPerPage;
            int end = Math.min(start + slotsPerPage, inventory.size());

            List<ItemStack> items = new ArrayList<>();
            for (int i = start; i < end; i++) {
                items.add(inventory.get(i));
            }
            return items;
        }

        public int getTotalPages() {
            return (int) Math.ceil((double) inventory.size() / slotsPerPage);
        }

        // Getters
        public String getName() { return name; }
        public NonNullList<ItemStack> getInventory() { return inventory; }

        public int getSlotsPerPage() {
            return slotsPerPage;
        }

        public ResourceLocation getTexture() {
            return texture;
        }
    }
    private static class DynamicSlot extends SlotItemHandler {
        private final CombatDepotMenu menu;
        private boolean hasBeenTaken = false;

        public DynamicSlot(ItemStackHandler handler, int index, int x, int y, CombatDepotMenu menu) {
            super(handler, index, x, y);
            this.menu = menu;
        }

        @Override
        public boolean mayPlace(@NotNull ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player playerIn) {
            // 检查是否已经被取出过
            if (hasBeenTaken) {
                // 如果已经被取出过，则只允许创造模式或OP权限的玩家取出
                return playerIn.isCreative() || playerIn.hasPermissions(2);
            }
            // 如果还没有被取出过，任何人都可以取出
            return true;
        }

        @Override
        public void onTake(@NotNull Player pPlayer, @NotNull ItemStack pStack) {
            super.onTake(pPlayer, pStack);
            // 标记这个槽位已经被取出过
            hasBeenTaken = true;
            menu.markSlotTaken(this.getSlotIndex());
        }

        @Override
        public void set(@NotNull ItemStack stack) {
            super.set(stack);

            ICard currentCard = menu.getCurrentCard();
            if (currentCard != null) {
                int actualIndex = menu.getCurrentPage() * SLOT_SIZE + getSlotIndex();
                NonNullList<ItemStack> inventory = currentCard.getInventory();

                // 确保索引在有效范围内
                if (actualIndex >= 0 && actualIndex < inventory.size()) {
                    inventory.set(actualIndex, stack.copy());
                } else {
                    CombatDepot.LOGGER.error("无效槽位索引: {} (库存大小: {})", actualIndex, inventory.size());
                }
            }

            int actualIndex = menu.getCurrentPage() * SLOT_SIZE + getSlotIndex();

            if (menu.getWorld().isClientSide()) { // 仅在客户端触发
                if (currentCard != null) {
                    CombatDepot.PACKET_HANDLER.sendToServer(
                            new UpdateSlotMessage(
                                    currentCard.getName(),
                                    actualIndex,
                                    stack.copy()
                            )
                    );
                }
            }

            if (menu.getWorld().isClientSide()) {
                if (currentCard != null) {
                    CombatDepot.PACKET_HANDLER.sendToServer(
                            new UpdateSlotMessage(
                                    currentCard.getName(),
                                    actualIndex,
                                    stack.copy()
                            )
                    );
                }
            }

            menu.broadcastChanges();
        }
    }
}