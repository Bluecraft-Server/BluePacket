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
    public DistributionMenu(int windowId, Inventory playerInventory, int selectedCard, int page) {
        super(windowId, playerInventory, selectedCard, page);
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

    @Override
    public boolean moveItemStackTo(ItemStack stack, int startIndex, int endIndex, boolean reverseDirection) {
        if (!stack.isEmpty()) {
            return super.moveItemStackTo(stack.copy(), startIndex, endIndex, reverseDirection);
        }
        return false;
    }

    @Override
    public void clicked(int slotId, int dragType, @NotNull ClickType clickType, @NotNull Player player) {
        if (slotId >= 0 && slotId < this.slots.size()) {
            Slot slot = this.slots.get(slotId);

            // 处理快捷键点击
            if (clickType == ClickType.SWAP && slot instanceof DistributionCardSlot) {
                if (dragType >= 0 && dragType < 9) {  // 数字键 1-9
                    // 获取快捷栏槽位
                    Slot hotbarSlot = this.slots.get(this.slots.size() - (9 - dragType));

                    // 检查是否可以取出
                    if (slot.mayPickup(player)) {
                        ItemStack slotStack = slot.getItem();
                        if (!slotStack.isEmpty()) {
                            ItemStack copy = slotStack.copy();
                            copy.setCount(Math.min(copy.getMaxStackSize(), copy.getCount()));

                            // 设置到快捷栏
                            hotbarSlot.set(copy);

                            // 如果不是管理员，减少剩余次数
                            if (!player.hasPermissions(2) && slot instanceof DistributionCardSlot distributionSlot) {
                                ICard card = getCurrentCard();
                                if (card instanceof Card card1) {
                                    card1.decrementRemainingCount(distributionSlot.getGlobalIndex());
                                }
                            }
                        }
                    }
                    return;
                }
            }
        }

        // 其他点击类型使用默认处理
        super.clicked(slotId, dragType, clickType, player);
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        ItemStack itemstack;
        Slot slot = this.slots.get(index);

        if (slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            itemstack = stackInSlot.copy();

            ICard card = getCurrentCard();
            // 从卡片槽位移动到玩家物品栏
            if (index < MenuConstants.SLOTS_PER_PAGE && card instanceof Card card1) {
                if (slot instanceof DistributionCardSlot distributionSlot) {
                    boolean isAdmin = player.hasPermissions(2);

                    if (!isAdmin) {
                        if (!card1.canExtract(distributionSlot.getGlobalIndex())) {
                            return ItemStack.EMPTY;
                        }
                    }

                    // 创建一个新的ItemStack用于移动，只设置一组物品的数量
                    ItemStack toMove = itemstack.copy();

                    // 寻找第一个可以放入的槽位
                    for (int i = MenuConstants.PLAYER_INVENTORY_START; i < MenuConstants.PLAYER_INVENTORY_END; i++) {
                        Slot targetSlot = this.slots.get(i);
                        if (!targetSlot.hasItem()) {
                            targetSlot.set(toMove);
                            // 如果移动成功且不是管理员，减少剩余次数
                            if (!isAdmin) {
                                card1.decrementRemainingCount(distributionSlot.getGlobalIndex());
                            }
                            return itemstack;
                        } else if (ItemStack.isSameItemSameTags(targetSlot.getItem(), toMove)) {
                            int space = targetSlot.getItem().getMaxStackSize() - targetSlot.getItem().getCount();
                            if (space > 0) {
                                int toAdd = Math.min(space, toMove.getCount());
                                targetSlot.getItem().grow(toAdd);
                                // 如果移动成功且不是管理员，减少剩余次数
                                if (!isAdmin) {
                                    card1.decrementRemainingCount(distributionSlot.getGlobalIndex());
                                }
                                return itemstack;
                            }
                        }
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

            ICard card = menu.getCurrentCard();
            int globalIndex = getGlobalIndex();

            if(card instanceof Card card1) {
                // 如果是管理员，可以无限取出
                if (isAdmin) {
                    ItemStack result = currentItem.copy();
                    result.setCount(Math.min(amount, currentItem.getCount()));
                    return result;
                }

                // 非管理员需要检查限制
                if (card1.canExtract(globalIndex)) {
                    ItemStack result = currentItem.copy();
                    result.setCount(Math.min(amount, currentItem.getCount()));
                    card1.decrementRemainingCount(globalIndex);
                    return result;
                }
            }

            return ItemStack.EMPTY;
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