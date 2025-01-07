package top.bluecraft.bluepacket.client;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import top.bluecraft.bluepacket.api.ICard;
import top.bluecraft.bluepacket.client.screen.GunViewScreen;
import top.bluecraft.bluepacket.common.card.Card;
import top.bluecraft.bluepacket.common.card.CardUtil;
import top.bluecraft.bluepacket.init.MenuRegistration;



@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {
        @SubscribeEvent
        public static void clientLoad(FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                MenuScreens.register(MenuRegistration.GUN_VIEW_MENU.get(),
                        (menu, inventory, component)
                                -> new GunViewScreen(menu, inventory, component, CardUtil.createCards()));
            });
        }
}