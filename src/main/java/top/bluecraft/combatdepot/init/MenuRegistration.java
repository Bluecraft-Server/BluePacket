package top.bluecraft.combatdepot.init;

import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import top.bluecraft.combatdepot.CombatDepot;
import top.bluecraft.combatdepot.common.inventory.menu.CombatDepotMenu;

public class MenuRegistration {
    public static final DeferredRegister<MenuType<?>> REGISTRATION = DeferredRegister.create(ForgeRegistries.MENU_TYPES, CombatDepot.MODID);

    public static final RegistryObject<MenuType<CombatDepotMenu>> GUN_VIEW_MENU = REGISTRATION.register("gun_view_menu", () -> new MenuType<>((id, inv) -> new CombatDepotMenu(id, inv, 0, 0), FeatureFlags.DEFAULT_FLAGS));
}
