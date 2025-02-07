package top.bluecraft.combatdepot.init;

import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import top.bluecraft.combatdepot.CombatDepot;
import top.bluecraft.combatdepot.common.item.GeneralTerminal;

public class ItemRegistration {
    public static final DeferredRegister<Item> REGISTRATION = DeferredRegister.create(ForgeRegistries.ITEMS, CombatDepot.MODID);

    public static final RegistryObject<Item> GENERAL_TERMINAL = REGISTRATION.register("general_terminal", GeneralTerminal::new);
}
