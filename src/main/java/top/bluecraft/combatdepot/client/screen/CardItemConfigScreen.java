package top.bluecraft.combatdepot.client.screen;

import net.minecraft.ResourceLocationException;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import top.bluecraft.combatdepot.CombatDepot;
import top.bluecraft.combatdepot.api.ICard;
import top.bluecraft.combatdepot.common.inventory.menu.CombatDepotMenu;
import top.bluecraft.combatdepot.network.AddItemToCardMessage;
import top.bluecraft.combatdepot.network.SyncCardsPacket;
import top.bluecraft.combatdepot.util.ItemHooks;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class CardItemConfigScreen extends Screen {
    private final CardEditScreen parentScreen;
    private final ICard card;
    private EditBox itemInput;
    private EditBox countInput;
    private EditBox deleteSlotInput;  // 新增：用于输入要删除的槽位索引
    private ConfigSlot itemSlot;
    private final List<Slot> inventorySlots = new ArrayList<>();

    // 添加物品栏相关常量
    private static final int INVENTORY_START_X = 0;
    private static final int INVENTORY_START_Y = 0;
    private static final int SLOT_SIZE = 18;
    private static final int SLOT_SPACING = 2;

    public CardItemConfigScreen(CardEditScreen parentScreen, ICard card) {
        super(Component.translatable("gui." + CombatDepot.MODID + ".card.item.config"));
        this.parentScreen = parentScreen;
        this.card = card;
    }

    @Override
    protected void init() {
        super.init();

        // 计算物品栏起始位置（居中）
        int invStartX = (width - 9 * SLOT_SIZE) / 2;
        int invStartY = height - 90;

        // 添加玩家背包槽位（3行9列）
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int x = invStartX + col * (SLOT_SIZE + SLOT_SPACING);
                int y = invStartY + row * (SLOT_SIZE + SLOT_SPACING);
                int index = col + (row + 1) * 9;

                if (minecraft != null) {
                    if (minecraft.player != null) {
                        inventorySlots.add(new Slot(minecraft.player.getInventory(), index, x, y));
                    }
                }
            }
        }

        // 添加快捷栏槽位（1行9列）
        int hotbarY = invStartY + 3 * (SLOT_SIZE + SLOT_SPACING) + 5;
        for (int col = 0; col < 9; col++) {
            int x = invStartX + col * (SLOT_SIZE + SLOT_SPACING);
            inventorySlots.add(new Slot(minecraft.player.getInventory(), col, x, hotbarY));
        }

        // 添加搜索框
        this.itemInput = new EditBox(font,
                width / 2 - 100, height / 2 - 80, 110, 20,
                Component.literal("minecraft:item_id"));
        this.itemInput.setSuggestion("modid:item_id");
        this.countInput = new EditBox(font,
                width / 2 + 20, height / 2 - 80, 60, 20,
                Component.literal("count"));
        this.countInput.setSuggestion("count");

        // 新增：删除槽位输入框
        this.deleteSlotInput = new EditBox(font,
                width / 2 + 20, height / 2 - 40, 60, 20,
                Component.literal("slot index"));
        this.deleteSlotInput.setSuggestion("slot index");

        addRenderableWidget(itemInput);
        addRenderableWidget(countInput);
        addRenderableWidget(deleteSlotInput);  // 添加删除槽位输入框

        // 新增：删除按钮
        addRenderableWidget(Button.builder(Component.literal("D"), button -> {
                    try {
                        int slotIndex = Integer.parseInt(deleteSlotInput.getValue());
                        NonNullList<ItemStack> inventory = card.getInventory();

                        if (slotIndex >= 0 && slotIndex < inventory.size()) {
                            // 将指定槽位的物品设为空
                            inventory.set(slotIndex, ItemStack.EMPTY);

                            // 发送更新包到服务器
                            if (this.minecraft != null && this.minecraft.player != null &&
                                    this.minecraft.level != null && this.minecraft.level.isClientSide() &&
                                    this.minecraft.player.getServer() != null && minecraft.player.containerMenu instanceof CombatDepotMenu menu) {
                                CombatDepot.PACKET_HANDLER.sendToServer(new AddItemToCardMessage(
                                        menu.getCards().get(menu.getSelectedCardIndex()).getName(),
                                        slotIndex,
                                        ItemStack.EMPTY  // 发送空物品栈
                                ));
                            }

                            updateInventorySerialize();
                            deleteSlotInput.setValue("");
                            minecraft.player.sendSystemMessage(Component.literal("物品已成功删除"));
                        } else {
                            minecraft.player.sendSystemMessage(Component.literal("无效的槽位索引"));
                        }
                    } catch (NumberFormatException e) {
                        minecraft.player.sendSystemMessage(Component.literal("请输入有效的槽位索引数字"));
                    } catch (Exception e) {
                        minecraft.player.sendSystemMessage(Component.literal("删除物品时发生错误"));
                    }
                })
                .pos(width / 2 - 10 , height / 2 - 40)
                .size(20, 20)
                .build());

        if (this.minecraft.player.containerMenu instanceof CombatDepotMenu menu) {
            this.itemSlot = new ConfigSlot(width / 2 - 100, height / 2 - 40, 0, this.font, menu);
        }

        // 添加统一的添加按钮（用于搜索框和配置槽）
        addRenderableWidget(Button.builder(Component.literal("+"), button -> {
                    if (!itemInput.getValue().isEmpty() && !countInput.getValue().isEmpty()) {
                        // 如果搜索框有输入，使用搜索框的添加逻辑
                        try {
                            // 先验证数量
                            int count;
                            try {
                                count = Integer.parseInt(countInput.getValue());
                                if (count <= 0 || count > 64) {
                                    minecraft.player.sendSystemMessage(Component.literal("物品数量必须在1-64之间"));
                                    return;
                                }
                            } catch (NumberFormatException e) {
                                minecraft.player.sendSystemMessage(Component.literal("请输入有效的物品数量"));
                                return;
                            }

                            // 验证物品ID
                            try {
                                ResourceLocation itemId = new ResourceLocation(itemInput.getValue());
                                Item item = ItemHooks.getItemOrThrow(itemId.getNamespace(), itemId.getPath());

                                ItemStack itemStack = new ItemStack(item, count);
                                addItemToCard(itemStack);
                                updateInventorySerialize();
                                itemInput.setValue("");
                                countInput.setValue("");
                            } catch (ResourceLocationException e) {
                                minecraft.player.sendSystemMessage(Component.literal("物品命名空间格式无效"));
                            } catch (Exception e) {
                                minecraft.player.sendSystemMessage(Component.literal("找不到指定的物品"));
                            }
                        } catch (Exception e) {
                            minecraft.player.sendSystemMessage(Component.literal("添加物品时发生错误"));
                        }
                    } else if (!itemSlot.getItem().isEmpty()) {
                        // 如果配置槽有物品，使用配置槽的添加逻辑
                        addItemToCard(itemSlot.getItem());
                        itemSlot.set(ItemStack.EMPTY);
                        updateInventorySerialize();
                    }
                })
                .pos(width / 2 + 85, height / 2 - 40)
                .size(20, 20)
                .build());

        // 返回按钮
        addRenderableWidget(Button.builder(
                        Component.translatable("gui." + CombatDepot.MODID + ".return"),
                        button -> {
                            if (minecraft != null) {
                                minecraft.setScreen(parentScreen);
                            }
                        })
                .pos(width / 2 - 200, height - 25)
                .size(100, 20)
                .build());
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTicks);

        // 渲染标题
        graphics.drawCenteredString(font, title, width / 2, 20, 0xFFFFFF);

        // 渲染物品栏槽位
        for (Slot slot : inventorySlots) {
            renderSlot(graphics, slot);
            // 如果鼠标悬停在槽位上，渲染物品提示
            if (isMouseOverSlot(mouseX, mouseY, slot) && slot.hasItem()) {
                graphics.renderTooltip(font, slot.getItem(), mouseX, mouseY);
            }
        }

        // 渲染配置槽
        itemSlot.render(graphics, mouseX, mouseY);

        // 渲染文本提示
        graphics.drawString(font,
                Component.translatable("gui." + CombatDepot.MODID + ".card.item.config.search"),
                width / 2 - 100, height / 2 - 95,
                0xFFFFFF);
        graphics.drawString(font,
                Component.translatable("gui." + CombatDepot.MODID + ".card.item.config.slot"),
                width / 2 - 100, height / 2 - 55,
                0xFFFFFF);
    }

    private void renderSlot(GuiGraphics graphics, Slot slot) {
        // 渲染槽位背景
        graphics.fill(slot.x, slot.y, slot.x + SLOT_SIZE, slot.y + SLOT_SIZE, 0xFF8B8B8B);
        graphics.fill(slot.x + 1, slot.y + 1, slot.x + SLOT_SIZE - 1, slot.y + SLOT_SIZE - 1, 0xFF373737);

        // 渲染槽位中的物品
        if (slot.hasItem()) {
            graphics.renderItem(slot.getItem(), slot.x + 1, slot.y + 1);
            graphics.renderItemDecorations(font, slot.getItem(), slot.x + 1, slot.y + 1);
        }
    }

    private boolean isMouseOverSlot(double mouseX, double mouseY, Slot slot) {
        return mouseX >= slot.x && mouseX < slot.x + SLOT_SIZE &&
                mouseY >= slot.y && mouseY < slot.y + SLOT_SIZE;
    }

    private void addItemToCard(ItemStack item) {
        NonNullList<ItemStack> inventory = card.getInventory();
        CombatDepotMenu menu = parentScreen.getParentScreen().getParentScreen().getMenu();

        // 获取当前页面的起始槽位
        int pageStart = menu.getCurrentPage() * SLOT_SIZE;
        int pageEnd = Math.min(pageStart + SLOT_SIZE, inventory.size());

        // 优先使用手动指定的槽位（需在页面范围内）
        try {
            String slotIndexStr = deleteSlotInput.getValue().trim();
            if (!slotIndexStr.isEmpty()) {
                int inputSlot = Integer.parseInt(slotIndexStr);
                if (inputSlot >= 0 && inputSlot < SLOT_SIZE) { // 使用页面局部索引
                    int globalSlot = pageStart + inputSlot;
                    if (globalSlot < inventory.size()) {
                        inventory.set(globalSlot, item);
                        sendUpdatePacket(globalSlot, item, menu);
                        return;
                    }
                }
            }
        } catch (NumberFormatException ignored) {}

        // 自动寻找当前页面的空槽位
        for (int localIndex = 0; localIndex < SLOT_SIZE; localIndex++) {
            int globalIndex = pageStart + localIndex;
            if (globalIndex >= pageEnd) break; // 确保不超过页面范围

            if (inventory.get(globalIndex).isEmpty()) {
                inventory.set(globalIndex, item);
                sendUpdatePacket(globalIndex, item, menu);
                return;
            }
        }

        // 提示无可用槽位
        if (minecraft != null && minecraft.player != null) {
            minecraft.player.sendSystemMessage(Component.literal("当前页面已满"));
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 检查是否点击了物品栏槽位
        for (Slot slot : inventorySlots) {
            if (isMouseOverSlot(mouseX, mouseY, slot) && slot.hasItem()) {
                itemSlot.set(slot.getItem().copy());
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void sendUpdatePacket(int globalSlot, ItemStack item, CombatDepotMenu menu) {
        CombatDepot.PACKET_HANDLER.sendToServer(new AddItemToCardMessage(
                card.getName(),
                globalSlot, // 使用全局槽位索引
                item.copy()
        ));

        // 强制刷新显示库存
        menu.syncDisplayInventory();
    }

    private void updateInventorySerialize() {
        if (this.minecraft != null &&
                this.minecraft.player != null &&
                this.minecraft.player.containerMenu instanceof CombatDepotMenu menu) {

            // 1. 获取当前选中的卡片
            ICard currentCard = menu.getCurrentCard();
            if (currentCard == null) return;

            // 5. 同步到客户端（如果需要）
            CombatDepot.PACKET_HANDLER.sendToServer(
                    new SyncCardsPacket(menu.getCards(), menu.getCardOffset())
            );
        }
    }

    public CardEditScreen getParentScreen() {
        return parentScreen;
    }
}