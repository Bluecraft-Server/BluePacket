package top.bluecraft.bluepacket.network;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import top.bluecraft.bluepacket.BluePacket;

@Mod.EventBusSubscriber(modid = BluePacket.MODID)
public class NetworkRegistry {
    @SubscribeEvent
    public static void registerMessage(FMLCommonSetupEvent event) {
        BluePacket.addNetworkMessage(GunViewSlotMessage.class, GunViewSlotMessage::buffer, GunViewSlotMessage::new, GunViewSlotMessage::handle);
        BluePacket.addNetworkMessage(GunViewChangeMessage.class, GunViewChangeMessage::buffer, GunViewChangeMessage::new, GunViewChangeMessage::handle);
    }

    public static <T> void sendMessage(T message) {
        BluePacket.PACKET_HANDLER.sendToServer(message);
    }
}
