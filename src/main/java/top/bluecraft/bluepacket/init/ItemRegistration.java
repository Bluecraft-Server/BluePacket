package top.bluecraft.bluepacket.init;

import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import top.bluecraft.bluepacket.BluePacket;
import top.bluecraft.bluepacket.common.item.GeneralTerminal;

public class ItemRegistration {
    public static final DeferredRegister<Item> REGISTRATION = DeferredRegister.create(ForgeRegistries.ITEMS, BluePacket.MODID);

    public static final RegistryObject<Item> GENERAL_TERMINAL= REGISTRATION.register("general_terminal", GeneralTerminal::new);
}
