package top.bluecraft.combatdepot.common.card;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import top.bluecraft.combatdepot.CombatDepot;
import top.bluecraft.combatdepot.api.ICardInventory;

import java.util.function.Consumer;

public class GeneralCard extends Card {
    public static final ResourceLocation BG_RESOURCE = new ResourceLocation(CombatDepot.MODID, "textures/gui/general.png");

    public GeneralCard(ICardInventory inventory, String name, ResourceLocation overlayTexture, Consumer<Integer> onPageChange) {
        super(inventory, name, overlayTexture, onPageChange);
    }

    @Override
    protected ResourceLocation getBackgroundTexture() {
        return BG_RESOURCE;
    }

    @Override
    public Component getTranslatableComponent() {
        return Component.translatable("gui." + CombatDepot.MODID + "." + getName());
    }
}
