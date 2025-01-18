package top.bluecraft.viewlauncher.init;

import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import top.bluecraft.viewlauncher.CombatDepot;
import top.bluecraft.viewlauncher.common.item.GeneralTerminal;

public class ItemRegistration {
    public static final DeferredRegister<Item> REGISTRATION = DeferredRegister.create(ForgeRegistries.ITEMS, CombatDepot.MODID);

    public static final RegistryObject<Item> GENERAL_TERMINAL= REGISTRATION.register("general_terminal", GeneralTerminal::new);
}
