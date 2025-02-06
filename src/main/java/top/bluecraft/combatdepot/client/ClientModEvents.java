package top.bluecraft.combatdepot.client;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import top.bluecraft.combatdepot.client.screen.CombatDepotScreen;
import top.bluecraft.combatdepot.init.MenuRegistration;



@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {
        @SubscribeEvent
        public static void clientLoad(FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                MenuScreens.register(MenuRegistration.GUN_VIEW_MENU.get(), CombatDepotScreen::new);
            });
        }
}