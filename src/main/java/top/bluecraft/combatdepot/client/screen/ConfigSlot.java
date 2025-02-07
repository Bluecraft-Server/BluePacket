package top.bluecraft.combatdepot.client.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;
import top.bluecraft.combatdepot.common.inventory.menu.CombatDepotMenu;

public class ConfigSlot extends SlotItemHandler {
    private final int x, y;
    private final Font font;
    private ItemStack stack = ItemStack.EMPTY;

    public ConfigSlot(int x, int y, int index, Font font, CombatDepotMenu menu) {
        super(new ItemStackHandler(1), index, x, y);
        this.x = x;
        this.y = y;
        this.font = font;
    }

    public void set(@NotNull ItemStack stack) {
        this.stack = stack;
    }

    public @NotNull ItemStack getItem() {
        return stack;
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.fill(x, y, x + 18, y + 18, 0xFF8B8B8B);
        graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF373737);
        if (!stack.isEmpty()) {
            graphics.renderItem(stack, x + 1, y + 1);
            graphics.renderItemDecorations(font, stack, x + 1, y + 1);
        }
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + 18 &&
                mouseY >= y && mouseY < y + 18;
    }
}
