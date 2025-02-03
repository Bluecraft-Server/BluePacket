package top.bluecraft.combatdepot.common.inventory.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import top.bluecraft.combatdepot.CombatDepot;
import top.bluecraft.combatdepot.common.data.GlobalCardStorage;
import top.bluecraft.combatdepot.config.CardConfig;
import top.bluecraft.combatdepot.init.MenuRegistration;
import top.bluecraft.combatdepot.network.SlotTakeMessage;
import top.bluecraft.combatdepot.network.SyncCardsPacket;

import java.util.*;

public class GunViewMenu extends AbstractContainerMenu {
    // Region: Constants
    private static final int GRID_ROWS = 10;
    private static final int GRID_COLS = 11;
    public static final int SLOT_SIZE = 110; // 10*11 - 最后一个槽位
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
    private List<Card> cards = new ArrayList<>();
    private int selectedCardIndex = -1;
    private int currentPage = 0;
    private int cardOffset = 0;

    // 显示用临时库存（当前页面）
    private final ItemStackHandler displayHandler = new ItemStackHandler(SLOT_SIZE);
    private final BitSet takenSlots = new BitSet(SLOT_SIZE);

    public GunViewMenu(int id, Inventory playerInventory) {
        super(MenuRegistration.GUN_VIEW_MENU.get(), id);
        this.player = playerInventory.player;
        this.world = player.level();
        BlockPos pos = player.blockPosition();
        this.x = pos.getX();
        this.y = pos.getY();
        this.z = pos.getZ();

        // 初始化卡牌数据（客户端和服务端都需要）
        CardConfig config = CardConfig.load();
        this.cards = config.createCards();

        if (!world.isClientSide) {
            ServerPlayer serverPlayer = (ServerPlayer) player;
            GlobalCardStorage storage = GlobalCardStorage.get(serverPlayer.serverLevel());

            for (Card card : cards) {
                // 强制覆盖库存数据
                ItemStackHandler handler = new ItemStackHandler(card.getInventory().getSlots());
                storage.updatePlayerInventory(serverPlayer, card.getName(), handler);
                card.setInventory(handler);
            }

            // 同步数据到客户端
            CombatDepot.PACKET_HANDLER.send(
                    PacketDistributor.PLAYER.with(() -> (ServerPlayer) player),
                    new SyncCardsPacket(cards, cardOffset)
            );
        }

        setupSlots(playerInventory);
        if (!cards.isEmpty()) selectCard(0);
    }

    public void markSlotTaken(int slotIndex) {
        takenSlots.set(slotIndex);
        broadcastChanges();
    }

    public boolean isSlotTaken(int slotIndex) {
        return takenSlots.get(slotIndex);
    }

    // Region: Core Functionality
    public void selectCard(int index) {
        if (index >= 0 && index < cards.size()) {
            this.selectedCardIndex = index;
            this.currentPage = 0;
            refreshDisplayInventory();
            broadcastChanges();
        }
    }

    // 在 GunViewMenu 类中添加以下方法

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

        Card card = cards.get(selectedCardIndex);
        if (page >= 0 && page < card.getTotalPages()) {
            this.currentPage = page;
            refreshDisplayInventory();
            broadcastChanges();
        }
    }

    private void refreshDisplayInventory() {
        if (selectedCardIndex == -1) return;

        Card card = cards.get(selectedCardIndex);
        List<ItemStack> pageItems = card.getPageItems(currentPage);

        // 清空显示库存
        for (int i = 0; i < SLOT_SIZE; i++) {
            displayHandler.setStackInSlot(i, ItemStack.EMPTY);
        }

        // 加载当前页数据
        for (int i = 0; i < Math.min(pageItems.size(), SLOT_SIZE); i++) {
            displayHandler.setStackInSlot(i, pageItems.get(i).copy());
        }
    }

    // Region: Slot Management
    private void setupSlots(Inventory playerInventory) {
        // 网格槽位
        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLS; col++) {
                int index = row * GRID_COLS + col;
                if (index >= SLOT_SIZE) break;

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
    public ItemStack quickMoveStack(Player playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = (Slot) this.slots.get(index);
        if (slot != null && slot.hasItem()) {
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

    private void savePersistentData() {
        ServerPlayer serverPlayer = (ServerPlayer) player;
        GlobalCardStorage storage = GlobalCardStorage.get(serverPlayer.serverLevel());

        cards.forEach(card -> {
            ItemStackHandler handler = card.getInventory();
            storage.updatePlayerInventory(serverPlayer, card.getName(), handler);
        });

        storage.setDirty();
    }

    public void setCardOffset(int offset) {
        this.cardOffset = offset;
        broadcastChanges(); // 通知客户端更新
    }

    // Region: Getters
    public List<Card> getCards() {
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

    // Region: Helper Classes
    public static class Card {
        private final String name;
        private ItemStackHandler inventory;
        private final int slotsPerPage;
        private final ResourceLocation texture;

        public Card(CardConfig.CardEntry entry, ItemStackHandler inventory) {
            this.name = entry.getName();
            this.inventory = inventory;
            this.slotsPerPage = SLOT_SIZE;
            this.texture = entry.getTexture();
        }

        public List<ItemStack> getPageItems(int page) {
            int start = page * slotsPerPage;
            int end = Math.min(start + slotsPerPage, inventory.getSlots());

            List<ItemStack> items = new ArrayList<>();
            for (int i = start; i < end; i++) {
                items.add(inventory.getStackInSlot(i));
            }
            return items;
        }

        public int getTotalPages() {
            return (int) Math.ceil((double) inventory.getSlots() / slotsPerPage);
        }

        // Getters
        public String getName() { return name; }
        public ItemStackHandler getInventory() { return inventory; }

        public void setInventory(ItemStackHandler inventory) {
            this.inventory = inventory;
        }

        public int getSlotsPerPage() {
            return slotsPerPage;
        }

        public ResourceLocation getTexture() {
            return texture;
        }
    }

    // 动态槽位（处理显示库存）
    private static class DynamicSlot extends SlotItemHandler {
        private final GunViewMenu menu;
        public DynamicSlot(ItemStackHandler handler, int index, int x, int y, GunViewMenu menu) {
            super(handler, index, x, y);
            this.menu = menu;
        }

        @Override
        public boolean mayPlace(@NotNull ItemStack stack) {
            return false;
        }

        @Override
        public void setChanged() {
            super.setChanged();
            // 触发数据同步
            if (container instanceof GunViewMenu menu) {
                menu.broadcastChanges();
            }
        }

        @Override
        public void onTake(@NotNull Player player, @NotNull ItemStack stack) {
            if (!mayPickup(player)) {
                return;
            }
            super.onTake(player, stack);

            // 标记槽位已被取出
            menu.markSlotTaken(getSlotIndex());

            // 只在客户端发送消息
            if (player.level().isClientSide()) {
                CombatDepot.PACKET_HANDLER.sendToServer(new SlotTakeMessage(getSlotIndex()));
            }
        }
    }
}