package top.bluecraft.bluepacket.network;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import top.bluecraft.bluepacket.BluePacket;

@Mod.EventBusSubscriber(modid = BluePacket.MODID)
public class NetworkRegistry {
    @SubscribeEvent
    public static void registerMessage(FMLCommonSetupEvent event) {
        BluePacket.addNetworkMessage(SlotTakeMessage.class, SlotTakeMessage::encode, SlotTakeMessage::decode, SlotTakeMessage.Handler::handle);
        BluePacket.addNetworkMessage(ClientboundCardSelectionPacket.class, ClientboundCardSelectionPacket::encode, ClientboundCardSelectionPacket::decode, ClientboundCardSelectionPacket.Handler::handle);
        BluePacket.addNetworkMessage(CardSelectionMessage.class, CardSelectionMessage::encode, CardSelectionMessage::decode, CardSelectionMessage.Handler::handle);
        BluePacket.addNetworkMessage(AddItemToCardMessage.class, AddItemToCardMessage::encode, AddItemToCardMessage::decode, AddItemToCardMessage.Handler::handle);
    }
}
