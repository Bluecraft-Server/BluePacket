package top.bluecraft.bluepacket.init;

import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import top.bluecraft.bluepacket.BluePacket;
import top.bluecraft.bluepacket.client.menu.GunViewMenu;

public class MenuRegistration {
    public static final DeferredRegister<MenuType<?>> REGISTRATION = DeferredRegister.create(ForgeRegistries.MENU_TYPES, BluePacket.MODID);

    public static final RegistryObject<MenuType<GunViewMenu>> GUN_VIEW_MENU = REGISTRATION.register("gun_view_menu", () -> new MenuType<>(GunViewMenu::new, FeatureFlags.DEFAULT_FLAGS));
}
