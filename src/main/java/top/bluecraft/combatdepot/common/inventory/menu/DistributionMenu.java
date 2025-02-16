package top.bluecraft.combatdepot.common.inventory.menu;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import top.bluecraft.combatdepot.api.ICard;
import top.bluecraft.combatdepot.common.data.GlobalCardStorage;
import top.bluecraft.combatdepot.common.inventory.Card;
import top.bluecraft.combatdepot.config.Config;

public class DistributionMenu extends CombatDepotMenu {
    private final Player player;
    public DistributionMenu(int windowId, Inventory playerInventory, int selectedCard, int page) {
        super(windowId, playerInventory, selectedCard, page);
        this.player = playerInventory.player;
    }

    @Override
    protected void initializeSlots(Inventory playerInventory) {
        // 替换为分发槽位
        for (int row = 0; row < 10; row++) {
            for (int col = 0; col < 11; col++) {
                int index = col + row * 11;
                int x = 10 + col * 18;
                int y = 10 + row * 18;
                DistributionCardSlot slot = new DistributionCardSlot(this, index, x, y);
                this.cardSlots.add(slot);
                addSlot(slot);
            }
        }

        // 添加玩家物品栏槽位
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory,
                        col + (row + 1) * 9,
                        220 + col * 18,
                        111 + row * 18));
            }
        }

        // 添加玩家快捷栏槽位
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory,
                    col,
                    220 + col * 18,
                    169));
        }
    }

    // 在 DistributionMenu.java 中添加
    @Override
    public boolean moveItemStackTo(@NotNull ItemStack stack, int startIndex, int endIndex, boolean reverseDirection) {
        // 检查是否从分发槽位移动到玩家物品栏
        if (startIndex < MenuConstants.SLOTS_PER_PAGE) {
            if (!player.hasPermissions(2)) {
                Slot slot = this.slots.get(startIndex);
                if (slot instanceof DistributionCardSlot distributionSlot) {
                    ICard card = getCurrentCard();
                    if (card instanceof Card card1) {
                        if (!card1.canExtract(distributionSlot.getGlobalIndex())) {
                            return false;
                        }
                        // 如果可以移动，在成功移动后减少次数
                        boolean moved = super.moveItemStackTo(stack, startIndex, endIndex, reverseDirection);
                        if (moved) {
                            card1.decrementRemainingCount(distributionSlot.getGlobalIndex());
                            // 保存数据
                            if (!getWorld().isClientSide()) {
                                GlobalCardStorage storage = GlobalCardStorage.get((ServerLevel) getWorld());
                                storage.updateInventory(card.getName(), card1);
                                storage.setDirty();
                                // 同步到客户端
                                syncDisplayInventory();
                            }
                        }
                        return moved;
                    }
                }
            }
        }
        return super.moveItemStackTo(stack, startIndex, endIndex, reverseDirection);
    }

    @Override
    public void clicked(int slotId, int dragType, @NotNull ClickType clickType, @NotNull Player player) {
        if (slotId >= 0 && slotId < this.slots.size()) {
            Slot slot = this.slots.get(slotId);

            if (slot instanceof DistributionCardSlot distributionSlot) {
                if (!player.hasPermissions(2)) {  // 非管理员检查
                    GlobalCardStorage storage = GlobalCardStorage.get((ServerLevel) getWorld());
                    Card card = storage.getInventory(getCurrentCard().getName());
                    int globalIndex = distributionSlot.getGlobalIndex();

                    if (!card.canExtract(globalIndex)) {
                        return;  // 如果不能提取，直接返回
                    }

                    // 如果是快捷键操作，在操作完成后减少次数
                    boolean needDecrement = clickType == ClickType.PICKUP
                            || clickType == ClickType.QUICK_MOVE
                            || clickType == ClickType.SWAP;

                    super.clicked(slotId, dragType, clickType, player);

                    if (needDecrement) {
                        card.decrementRemainingCount(globalIndex);
                        storage.setDirty();
                        syncDisplayInventory();
                    }
                    return;
                }
            }
        }
        super.clicked(slotId, dragType, clickType, player);
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stackInSlot = slot.getItem();
        ItemStack result = stackInSlot.copy();

        if (index < MenuConstants.SLOTS_PER_PAGE) {  // 从卡片槽位到玩家物品栏
            if (!player.hasPermissions(2)) {  // 非管理员检查
                if (slot instanceof DistributionCardSlot distributionSlot) {
                    GlobalCardStorage storage = GlobalCardStorage.get((ServerLevel) getWorld());
                    Card card = storage.getInventory(getCurrentCard().getName());
                    int globalIndex = distributionSlot.getGlobalIndex();

                    if (!card.canExtract(globalIndex)) {
                        return ItemStack.EMPTY;
                    }

                    // 如果移动成功，减少剩余次数并保存
                    if (moveItemStackTo(stackInSlot, MenuConstants.PLAYER_INVENTORY_START,
                            MenuConstants.PLAYER_INVENTORY_END, true)) {
                        card.decrementRemainingCount(globalIndex);
                        storage.setDirty();
                        syncDisplayInventory();
                        return result;
                    }
                }
            }
        }
        return ItemStack.EMPTY;
    }

    public static class DistributionCardSlot extends CardSlot {
        private final int slotIndex;
        private final ItemStack displayedItem = ItemStack.EMPTY;

        public DistributionCardSlot(DistributionMenu menu, int index, int x, int y) {
            super(menu, index, x, y);
            this.slotIndex = index;
        }

        @Override
        public boolean mayPlace(@NotNull ItemStack stack) {
            return Config.debug; // 禁止放入物品
        }

        @Override
        public @NotNull ItemStack getItem() {
            // 始终从GlobalCardStorage获取最新数据
            if (menu.getCurrentCard() != null) {
                Level world = menu.getWorld();
                if (!world.isClientSide()) {
                    GlobalCardStorage storage = GlobalCardStorage.get((ServerLevel) world);
                    Card card = storage.getInventory(menu.getCurrentCard().getName());
                    int globalIndex = getGlobalIndex();
                    if (globalIndex < card.getInventory().size()) {
                        return card.getInventory().get(globalIndex).copy();
                    }
                }
            }
            return this.displayedItem;
        }

        @Override
        public @NotNull ItemStack remove(int amount) {
            Player player = menu.getPlayer();
            if (player == null) return ItemStack.EMPTY;

            // 检查管理员权限
            boolean isAdmin = player.hasPermissions(2);

            ItemStack currentItem = getItem();
            if (currentItem.isEmpty()) return ItemStack.EMPTY;

            if (!isAdmin) {  // 非管理员需要检查限制
                GlobalCardStorage storage = GlobalCardStorage.get((ServerLevel) menu.getWorld());
                Card card = storage.getInventory(menu.getCurrentCard().getName());
                int globalIndex = getGlobalIndex();

                if (!card.canExtract(globalIndex)) {
                    return ItemStack.EMPTY;
                }

                // 如果可以提取，创建返回的物品堆
                ItemStack result = currentItem.copy();
                result.setCount(Math.min(amount, currentItem.getCount()));

                // 减少提取次数并保存
                card.decrementRemainingCount(globalIndex);
                storage.setDirty();
                menu.syncDisplayInventory();

                return result;
            }

            // 管理员直接取出
            ItemStack result = currentItem.copy();
            result.setCount(Math.min(amount, currentItem.getCount()));
            return result;
        }

        @Override
        public boolean mayPickup(@NotNull Player player) {
            if (player.hasPermissions(2)) {
                // 管理员可以无限取出
                return !getItem().isEmpty();
            }

            // 非管理员需要检查限制
            ICard card = menu.getCurrentCard();
            if (card instanceof Card card1) {
                int globalIndex = getGlobalIndex();
                return card1.canExtract(globalIndex) && !getItem().isEmpty();
            }
            return false;
        }

        public int getGlobalIndex() {
            return menu.getCurrentPage() * CombatDepotMenu.getSlotSize() + slotIndex;
        }
    }
}