package top.bluecraft.combatdepot.init;

import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import top.bluecraft.combatdepot.CombatDepot;
import top.bluecraft.combatdepot.common.inventory.menu.CombatDepotMenu;
import top.bluecraft.combatdepot.common.inventory.menu.DistributionMenu;

public class MenuRegistration {
    public static final DeferredRegister<MenuType<?>> REGISTRATION = DeferredRegister.create(ForgeRegistries.MENU_TYPES, CombatDepot.MODID);

    public static final RegistryObject<MenuType<CombatDepotMenu>> COMBAT_DEPOT_MENU =
            REGISTRATION.register("combat_depot_menu", () -> new MenuType<>((id, inv) ->
                    new CombatDepotMenu(id, inv, 0, 0), FeatureFlags.DEFAULT_FLAGS));

    public static final RegistryObject<MenuType<DistributionMenu>> DISTRIBUTION_MENU =
            REGISTRATION.register("distrubtion_menu", () -> new MenuType<>((id, inv) ->
                    new DistributionMenu(id, inv, 0, 0), FeatureFlags.DEFAULT_FLAGS));
}
