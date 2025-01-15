package top.bluecraft.viewlauncher.common.capability;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.bluecraft.viewlauncher.ViewLauncher;
import top.bluecraft.viewlauncher.api.ICardInventory;

@Mod.EventBusSubscriber(modid = ViewLauncher.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModCapabilities {
    public static final Capability<ICardInventory> CARD_INVENTORY = CapabilityManager.get(new CapabilityToken<>(){});

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.register(ICardInventory.class);
    }
}