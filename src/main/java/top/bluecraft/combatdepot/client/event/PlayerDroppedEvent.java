package top.bluecraft.combatdepot.client.event;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.bluecraft.combatdepot.client.screen.GunViewScreen;
import top.bluecraft.combatdepot.init.ItemRegistration;

@Mod.EventBusSubscriber
public class PlayerDroppedEvent {
    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void onItemDropped(ItemTossEvent event) {
        if (event.getEntity().getItem().getItem() == ItemRegistration.GENERAL_TERMINAL.get()) {
            if (Minecraft.getInstance().screen instanceof GunViewScreen) {
                if (Minecraft.getInstance().player != null) {
                    Minecraft.getInstance().player.closeContainer();
                }
            }
        }
    }
}
