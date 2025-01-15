package top.bluecraft.viewlauncher.network;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import top.bluecraft.viewlauncher.ViewLauncher;

@Mod.EventBusSubscriber(modid = ViewLauncher.MODID)
public class NetworkRegistry {
    @SubscribeEvent
    public static void registerMessage(FMLCommonSetupEvent event) {
        ViewLauncher.addNetworkMessage(SlotTakeMessage.class, SlotTakeMessage::encode, SlotTakeMessage::decode, SlotTakeMessage.Handler::handle);
        ViewLauncher.addNetworkMessage(ClientboundCardSelectionPacket.class, ClientboundCardSelectionPacket::encode, ClientboundCardSelectionPacket::decode, ClientboundCardSelectionPacket.Handler::handle);
        ViewLauncher.addNetworkMessage(CardSelectionMessage.class, CardSelectionMessage::encode, CardSelectionMessage::decode, CardSelectionMessage.Handler::handle);
        ViewLauncher.addNetworkMessage(AddItemToCardMessage.class, AddItemToCardMessage::encode, AddItemToCardMessage::decode, AddItemToCardMessage.Handler::handle);
    }
}
