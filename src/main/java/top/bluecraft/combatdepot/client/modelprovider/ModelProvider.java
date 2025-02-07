package top.bluecraft.combatdepot.client.modelprovider;

import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import top.bluecraft.combatdepot.CombatDepot;
import top.bluecraft.combatdepot.init.ItemRegistration;

public class ModelProvider extends ItemModelProvider {

    private static final String GNR_TERMINAL_ID = ItemRegistration.GENERAL_TERMINAL.getId().getPath();

    public ModelProvider(PackOutput gen, ExistingFileHelper helper) {
        super(gen, CombatDepot.MODID, helper);
    }

    @Override
    protected void registerModels() {
        this.singleTexture(GNR_TERMINAL_ID, new ResourceLocation("item/generated"), "layer0", new ResourceLocation(CombatDepot.MODID, "item/" + GNR_TERMINAL_ID));
    }
}