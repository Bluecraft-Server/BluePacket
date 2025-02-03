package top.bluecraft.combatdepot.common.inventory.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import top.bluecraft.combatdepot.CombatDepot;
import top.bluecraft.combatdepot.api.ICardInventory;
import top.bluecraft.combatdepot.common.card.Cards;
import top.bluecraft.combatdepot.init.MenuRegistration;

import java.util.*;

public class GunViewMenu extends AbstractContainerMenu {
    // 常量定义
    private static final int GRID_ROWS = 10;
    private static final int GRID_COLS = 11;
    private static final int GRID_START_X = 10;
    private static final int GRID_START_Y = 10;
    private static final int SLOT_SPACING = 18;
    private static final int PLAYER_INV_START_X = 220;
    private static final int PLAYER_INV_START_Y = 111;
    private static final int HOTBAR_START_Y = 169;
    private static final int GRID_START_INDEX = 0;
    private static final int SLOT_SIZE = 110; // 10 * 10
    private static final int GRID_END_INDEX = GRID_START_INDEX + SLOT_SIZE - 1;
    public static final Map<String, ICardInventory> cardInventories = new HashMap<>();
    private final BitSet takenSlots;  // 记录已取出过物品的槽位

    // 游戏状态
    public final Level world;
    public final Player entity;
    public int x, y, z;
    private final List<ICardInventory> inventories;
    private int selectedCardIndex = 0;
    private final Map<Integer, List<ItemStack>> pageCache = new HashMap<>();

    // 物品管理
    public final Inventory inventory;

    public GunViewMenu(int id, Inventory playerInventory) {
        super(MenuRegistration.GUN_VIEW_MENU.get(), id);

        // 初始化基本属性
        this.entity = playerInventory.player;
        this.world = entity.level();
        this.inventory = playerInventory;
        this.takenSlots = new BitSet(SLOT_SIZE);

        this.inventories = Cards.CARD_INVENTORIES;

        // 初始化位置信息
        BlockPos pos = playerInventory.player.blockPosition();
        this.x = pos.getX();
        this.y = pos.getY();
        this.z = pos.getZ();


        // 设置槽位
        setupGridSlots();
        setupPlayerInventorySlots(playerInventory);
        for (int i = 0; i < Cards.CARD_INVENTORIES.size(); i++) {
            cardInventories.put(Cards.CARD_INVENTORIES.get(i).getName(), Cards.CARD_INVENTORIES.get(i));
        }
    }

    public void addItemToCard(int cardIndex, int slot, ItemStack stack) {
        ICardInventory inventory = inventories.get(cardIndex);
        if (inventory != null && slot >= 0 && slot < inventory.getSlots()) {
            inventory.setStackInSlot(slot, stack);
            broadcastChanges();
        }
    }

    public void requestCardSelection(int index) {
        if (index >= 0 && index < getInventories().size()) {
            selectedCardIndex = index;
            // Clear current page when switching cards
            clearCurrentPage();
            // Notify clients about card selection
            broadcastChanges();
        }
    }

    public void requestPageLoad(int pageIndex, List<ItemStack> items) {
        if (!world.isClientSide) {
            pageCache.put(pageIndex, items);
            loadCurrentPage(items);
        }
    }

    public void clearCurrentPage() {
        loadCurrentPage(Collections.emptyList());
    }

    public void loadCurrentPage(List<ItemStack> items) {
        for (int i = 0; i < SLOT_SIZE; i++) {
            inventories.get(selectedCardIndex).setStackInSlot(i, items != null && i < items.size() ?
                    items.get(i).copy() : ItemStack.EMPTY);
        }
        broadcastChanges();
    }

    public int getSelectedCardIndex() {
        return selectedCardIndex;
    }


    public void markSlotTaken(int slotIndex) {
        takenSlots.set(slotIndex);
        broadcastChanges();
    }

    public boolean isSlotTaken(int slotIndex) {
        return takenSlots.get(slotIndex);
    }



    private void saveMenuState(Player player) {
        CompoundTag tag = player.getUseItem().getOrCreateTag();
        tag.put("MenuState", saveState());
        inventories.get(selectedCardIndex).serializeNBT();

        // Save page cache
        CompoundTag cacheTag = new CompoundTag();
        pageCache.forEach((pageIndex, items) -> {
            ListTag itemList = new ListTag();
            items.forEach(stack -> {
                CompoundTag itemTag = new CompoundTag();
                stack.save(itemTag);
                itemList.add(itemTag);
            });
            cacheTag.put(String.valueOf(pageIndex), itemList);
        });
        tag.put("PageCache", cacheTag);
    }


    public CompoundTag saveState() {
        CompoundTag tag = new CompoundTag();
        byte[] bytes = takenSlots.toByteArray();
        tag.putByteArray("TakenSlots", bytes);
        return tag;
    }

    public void loadState(CompoundTag tag) {
        if (tag.contains("TakenSlots")) {
            byte[] bytes = tag.getByteArray("TakenSlots");
            takenSlots.clear();
            BitSet.valueOf(bytes).stream().forEach(takenSlots::set);
            broadcastChanges(); // 通知客户端更新
        }
    }

    // 保存数据
    public static CompoundTag saveInventories() {
        CompoundTag tag = new CompoundTag();
        cardInventories.forEach((name, inv) -> {
            // 为每个库存创建一个单独的CompoundTag
            CompoundTag inventoryTag = new CompoundTag();

            // 序列化库存数据
            inventoryTag.put("Data", inv.serializeNBT());

            // 添加库存类型信息，以便在加载时重新创建正确的库存类型
            inventoryTag.putString("Type", inv.getClass().getName());

            // 将库存标签添加到主标签
            tag.put(name, inventoryTag);
        });
        return tag;
    }

    // 加载数据
    public static void loadInventories(CompoundTag tag) {
        // 遍历现有的cardInventories
        cardInventories.forEach((name, inv) -> {
            if (tag.contains(name)) {
                CompoundTag inventoryTag = tag.getCompound(name);

                try {
                    // 如果存储了类型信息，可以进行更安全的反序列化
                    if (inventoryTag.contains("Type")) {
                        String inventoryTypeName = inventoryTag.getString("Type");
                        Class<?> inventoryClass = Class.forName(inventoryTypeName);

                        // 可以在这里添加额外的类型检查
                        if (inv.getClass().getName().equals(inventoryTypeName)) {
                            inv.deserializeNBT(inventoryTag.getCompound("Data"));
                        } else {
                            CombatDepot.LOGGER.warn("Inventory type mismatch for " + name +
                                    ": stored " + inventoryTypeName + ", current " + inv.getClass().getName());
                        }
                    } else {
                        // 如果没有类型信息，则直接反序列化
                        inv.deserializeNBT(inventoryTag);
                    }
                } catch (ClassNotFoundException e) {
                    CombatDepot.LOGGER.error("Could not find inventory class when loading: " + name, e);
                }
            }
        });
    }

    private void setupGridSlots() {
        int slotCount = 0;
        for (int row = 0; row < GRID_ROWS && slotCount < SLOT_SIZE; row++) {
            for (int col = 0; col < GRID_COLS && slotCount < SLOT_SIZE; col++) {
                // 直接使用slotCount作为索引
                int xPos = GRID_START_X + col * SLOT_SPACING;
                int yPos = GRID_START_Y + row * SLOT_SPACING;

                this.addSlot(new GunViewSlot(inventories.get(selectedCardIndex).getInventory(), slotCount, xPos, yPos, this));
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

    @Override
    public boolean stillValid(@NotNull Player player) {
        return true;
    }

    @Override
    public void removed(@NotNull Player player) {
        super.removed(player);
        if (!player.level().isClientSide) {
            saveMenuState(player);
        }
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

    public void selectCard(int index) {
        if (index >= 0 && index < inventories.size() && index != selectedCardIndex) {
            selectedCardIndex = index;
            loadCurrentPage(Collections.emptyList()); // 切换卡片时先清空当前页面
        }
    }

    public ItemStackHandler getItemHandler() {
        return inventories.get(selectedCardIndex).getInventory();
    }

    public BitSet getTakenSlots() {
        return takenSlots;
    }

    public List<ICardInventory> getInventories() {
        return inventories;
    }
}