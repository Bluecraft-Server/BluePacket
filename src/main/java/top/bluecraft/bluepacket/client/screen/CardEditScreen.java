package top.bluecraft.bluepacket.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import top.bluecraft.bluepacket.BluePacket;
import top.bluecraft.bluepacket.api.ICard;
import top.bluecraft.bluepacket.api.ICardInventory;
import top.bluecraft.bluepacket.common.page.Page;
import top.bluecraft.bluepacket.network.AddItemToCardMessage;
import top.bluecraft.bluepacket.util.ItemHooks;

import java.util.List;

public class CardEditScreen extends Screen {
    private final CardConfigScreen parentScreen;
    private final ICard card;
    private EditBox itemInput;
    private int currentPage = 0;

    public CardEditScreen(CardConfigScreen parentScreen, ICard card) {
        super(Component.translatable("gui.bluepacket.card.edit", card.getName()));
        this.parentScreen = parentScreen;
        this.card = card;
    }

    @Override
    protected void init() {
        super.init();

        // 添加物品输入框
        this.itemInput = new EditBox(font, width / 2 - 100, height - 60, 180, 20,
                Component.literal("minecraft:item_id"));
        this.itemInput.setSuggestion("modid:item_id");
        addRenderableWidget(itemInput);

        // 添加物品按钮
        addRenderableWidget(Button.builder(Component.literal("+"), (button) -> {
                    try {
                        ResourceLocation itemId = new ResourceLocation(itemInput.getValue());
                        Item item = ItemHooks.getItemOrThrow(itemId.getNamespace(), itemId.getPath());
                        addItemToCard(item);
                        itemInput.setValue("");
                    } catch (Exception e) {
                        // 显示错误信息
                    }
                })
                .pos(width / 2 + 85, height - 60)
                .size(20, 20)
                .build());

        // 添加翻页按钮
        addRenderableWidget(Button.builder(Component.literal("<"), (button) -> {
                    if (currentPage > 0) currentPage--;
                })
                .pos(width / 2 - 50, height / 2)
                .size(20, 20)
                .build());

        addRenderableWidget(Button.builder(Component.literal(">"), (button) -> {
                    if (currentPage < card.getTotalPages() - 1) currentPage++;
                })
                .pos(width / 2 + 30, height / 2)
                .size(20, 20)
                .build());

        // 返回按钮
        addRenderableWidget(Button.builder(Component.literal("返回"), (button) -> {
                    if (minecraft != null) {
                        minecraft.setScreen(parentScreen);
                    }
                })
                .pos(width / 2 - 50, height - 30)
                .size(100, 20)
                .build());
    }

    private void addItemToCard(Item item) {
        ItemStack stack = new ItemStack(item);
        ICardInventory inventory = card.getInventory(); // 假设你的ICard接口中有getInventory方法

        // 找到第一个空槽位
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack existingStack = inventory.getStackInSlot(i);
            if (existingStack.isEmpty()) {
                // 将物品放入该槽位
                inventory.setStackInSlot(i, stack);

                // 发送更新包到服务器
                BluePacket.PACKET_HANDLER.sendToServer(new AddItemToCardMessage(
                        card.getName(),  // 用于识别是哪个卡片
                        i,              // 槽位索引
                        item.getDefaultInstance()  // 物品
                ));

                break;
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTicks);

        // 渲染标题
        graphics.drawCenteredString(font, title, width / 2, 20, 0xFFFFFF);

        // 渲染页面信息
        Component pageInfo = Component.translatable("gui.bluepacket.card.edit.pages", currentPage + 1, card.getTotalPages());
        graphics.drawCenteredString(font, pageInfo, width / 2, height / 2 - 10, 0xFFFFFF);

        // 渲染当前页面的物品
        renderCardPage(graphics);
    }

    private void renderCardPage(GuiGraphics graphics) {
        Page page = card.getPage(currentPage);
        List<ItemStack> items = page.items();

        int startX = width / 2 - 90;
        int startY = 50;
        int itemSize = 16;
        int spacing = 20;

        for (int i = 0; i < Math.min(items.size(), 45); i++) {  // 显示前45个物品
            int row = i / 9;
            int col = i % 9;
            int x = startX + col * spacing;
            int y = startY + row * spacing;

            ItemStack stack = items.get(i);
            if (!stack.isEmpty()) {
                graphics.renderItem(stack, x, y);
            }
        }
    }
}
