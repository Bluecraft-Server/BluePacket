package top.bluecraft.bluepacket.client.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import top.bluecraft.bluepacket.BluePacket;
import top.bluecraft.bluepacket.api.ICard;
import top.bluecraft.bluepacket.api.ICardInventory;
import top.bluecraft.bluepacket.common.capability.CardInventoryCapability;
import top.bluecraft.bluepacket.common.card.GeneralCard;
import top.bluecraft.bluepacket.common.page.Page;
import top.bluecraft.bluepacket.init.MenuRegistration;

import java.util.*;

public class GunViewMenu extends AbstractContainerMenu {
    // 常量定义
    private static final int GRID_ROWS = 11;
    private static final int GRID_COLS = 10;
    private static final int GRID_START_X = 14;
    private static final int GRID_START_Y = 14;
    private static final int SLOT_SPACING = 18;
    private static final int PLAYER_INV_START_X = 220;
    private static final int PLAYER_INV_START_Y = 111;
    private static final int HOTBAR_START_Y = 169;
    private static final int GRID_START_INDEX = 0;
    private static final int SLOT_SIZE = 110; // 11 * 10
    private static final int GRID_END_INDEX = GRID_START_INDEX + SLOT_SIZE - 1;
    private final Map<String, ICardInventory> cardInventories = new HashMap<>();
    public static final ICardInventory MAIN_WEAPON_INV = new CardInventoryCapability(1100);

    // 游戏状态
    public final Level world;
    public final Player entity;
    public int x, y, z;
    public final List<ICard> cards;
    public int currentPageIndex = 0;
    public int selectedCardIndex = 0;
    public ICard currentCard;

    // 物品管理
    public final Inventory inventory;
    private final ItemStackHandler itemHandler;

    public GunViewMenu(int id, Inventory playerInventory) {
        super(MenuRegistration.GUN_VIEW_MENU.get(), id);

        // 初始化基本属性
        this.entity = playerInventory.player;
        this.world = entity.level();
        this.inventory = playerInventory;

        // 初始化位置信息
        BlockPos pos = playerInventory.player.blockPosition();
        this.x = pos.getX();
        this.y = pos.getY();
        this.z = pos.getZ();

        // 初始化物品处理器，使用完整大小以匹配槽位索引
        this.itemHandler = new ItemStackHandler(14700) {
            @Override
            public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                return slot >= GRID_START_INDEX && slot < GRID_END_INDEX + 1;
            }

            @Override
            protected void onContentsChanged(int slot) {
                super.onContentsChanged(slot);
                if (slot >= GRID_START_INDEX && slot <= GRID_END_INDEX) {
                    broadcastChanges();
                }
            }
        };

        // 初始化卡片系统
        this.cards = initializeCards();
        this.currentCard = !cards.isEmpty() ? cards.get(0) : null;

        // 设置槽位
        setupGridSlots();
        setupPlayerInventorySlots(playerInventory);

        // 初始化第一页物品
        if (currentCard != null) {
            loadCurrentPage();
        }
    }

    private List<ICard> initializeCards() {
        List<ICard> cardList = new ArrayList<>();

        cardInventories.put("main_weapon", MAIN_WEAPON_INV);

        cardList.add(new GeneralCard(MAIN_WEAPON_INV, "main_weapon",
                new ResourceLocation(BluePacket.MODID, "textures/gui/main_weapon.png"),
                this::onPageChanged));
        return cardList;
    }

    // 保存数据
    public CompoundTag saveInventories() {
        CompoundTag tag = new CompoundTag();
        cardInventories.forEach((name, inv) -> {
            tag.put(name, inv.serializeNBT());
        });
        return tag;
    }

    // 加载数据
    public void loadInventories(CompoundTag tag) {
        cardInventories.forEach((name, inv) -> {
            if (tag.contains(name)) {
                inv.deserializeNBT(tag.getCompound(name));
            }
        });
    }

    private void onPageChanged(int newPageIndex) {
        if (currentCard != null && newPageIndex >= 0 && newPageIndex < currentCard.getTotalPages()) {
            currentPageIndex = newPageIndex;
            loadCurrentPage();
        }
    }

    private void setupGridSlots() {
        int slotCount = 0;
        for (int row = 0; row < GRID_ROWS && slotCount < SLOT_SIZE; row++) {
            for (int col = 0; col < GRID_COLS && slotCount < SLOT_SIZE; col++) {
                // 直接使用slotCount作为索引
                int xPos = GRID_START_X + col * SLOT_SPACING;
                int yPos = GRID_START_Y + row * SLOT_SPACING;

                this.addSlot(new GunViewSlot(itemHandler, slotCount, xPos, yPos, this));
                slotCount++;
            }
        }
    }

    private void setupPlayerInventorySlots(Inventory playerInventory) {
        // 添加主物品栏槽位（3行9列）
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory,
                        col + (row + 1) * 9,
                        PLAYER_INV_START_X + col * SLOT_SPACING,
                        PLAYER_INV_START_Y + row * SLOT_SPACING));
            }
        }

        // 添加快捷栏槽位（1行9列）
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory,
                    col,
                    PLAYER_INV_START_X + col * SLOT_SPACING,
                    HOTBAR_START_Y));
        }
    }

    public void loadCurrentPage() {
        if (currentCard == null) return;

        Page page = currentCard.getPage(currentPageIndex);
        List<ItemStack> items = page.items();

        // 更新槽位，考虑偏移量
        for (int i = 0; i < Math.min(items.size(), SLOT_SIZE); i++) {
            itemHandler.setStackInSlot(i + GRID_START_INDEX, items.get(i).copy());
        }

        // 清空剩余槽位
        for (int i = items.size(); i < SLOT_SIZE; i++) {
            itemHandler.setStackInSlot(i + GRID_START_INDEX, ItemStack.EMPTY);
        }

        broadcastChanges();
    }

    public void selectCard(int index) {
        if (index >= 0 && index < cards.size() && index != selectedCardIndex) {
            selectedCardIndex = index;
            currentCard = cards.get(index);
            currentPageIndex = 0;
            loadCurrentPage();
        }
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return true;
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            itemstack = slotStack.copy();

            if (index >= GRID_START_INDEX && index <= GRID_END_INDEX) {
                // 从网格移动到玩家物品栏
                if (!this.moveItemStackTo(slotStack, GRID_END_INDEX + 1, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // 从玩家物品栏移动到网格
                if (!this.moveItemStackTo(slotStack, GRID_START_INDEX, GRID_END_INDEX + 1, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (slotStack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return itemstack;
    }
}
