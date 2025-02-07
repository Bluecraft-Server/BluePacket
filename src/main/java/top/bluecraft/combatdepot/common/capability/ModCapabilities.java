package top.bluecraft.combatdepot.common.capability;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.bluecraft.combatdepot.CombatDepot;

@Mod.EventBusSubscriber(modid = CombatDepot.MODID)
public class ModCapabilities {
    // 定义 Capability 实例
    public static final Capability<GeneralTerminalInventoryCapability> GENERAL_TERMINAL_INVENTORY = CapabilityManager.get(new CapabilityToken<>() {
    });

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.register(GeneralTerminalInventoryCapability.class);
    }
}