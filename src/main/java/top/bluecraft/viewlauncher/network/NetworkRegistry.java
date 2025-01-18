package top.bluecraft.viewlauncher.network;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import top.bluecraft.viewlauncher.CombatDepot;

@Mod.EventBusSubscriber(modid = CombatDepot.MODID)
public class NetworkRegistry {
    @SubscribeEvent
    public static void registerMessage(FMLCommonSetupEvent event) {
        CombatDepot.addNetworkMessage(SlotTakeMessage.class, SlotTakeMessage::encode, SlotTakeMessage::decode, SlotTakeMessage.Handler::handle);
        CombatDepot.addNetworkMessage(ClientboundCardSelectionPacket.class, ClientboundCardSelectionPacket::encode, ClientboundCardSelectionPacket::decode, ClientboundCardSelectionPacket.Handler::handle);
        CombatDepot.addNetworkMessage(CardSelectionMessage.class, CardSelectionMessage::encode, CardSelectionMessage::decode, CardSelectionMessage.Handler::handle);
        CombatDepot.addNetworkMessage(AddItemToCardMessage.class, AddItemToCardMessage::encode, AddItemToCardMessage::decode, AddItemToCardMessage.Handler::handle);
    }
}
