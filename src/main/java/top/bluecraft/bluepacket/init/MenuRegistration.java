package top.bluecraft.bluepacket.init;

import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import top.bluecraft.bluepacket.BluePacket;
import top.bluecraft.bluepacket.client.menu.GunViewMenu;
import top.bluecraft.bluepacket.common.card.Cards;

import java.util.Arrays;

public class MenuRegistration {
    public static final DeferredRegister<MenuType<?>> REGISTRATION = DeferredRegister.create(ForgeRegistries.MENU_TYPES, BluePacket.MODID);

    public static final RegistryObject<MenuType<GunViewMenu>> GUN_VIEW_MENU = REGISTRATION.register("gun_view_menu", () -> IForgeMenuType.create((id, inv, data) -> new GunViewMenu(id, inv, data, Arrays.asList(Cards.CARDS))));
}
