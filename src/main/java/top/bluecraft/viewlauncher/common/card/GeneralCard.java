package top.bluecraft.viewlauncher.common.card;

import net.minecraft.resources.ResourceLocation;
import top.bluecraft.viewlauncher.CombatDepot;
import top.bluecraft.viewlauncher.api.ICardInventory;

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
}
