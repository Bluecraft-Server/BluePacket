package top.bluecraft.viewlauncher;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.data.LanguageProvider;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;
import top.bluecraft.viewlauncher.client.modelprovider.ModelProvider;
import top.bluecraft.viewlauncher.init.ItemRegistration;
import top.bluecraft.viewlauncher.init.MenuRegistration;
import top.bluecraft.viewlauncher.network.AddItemToCardMessage;
import top.bluecraft.viewlauncher.network.CardSelectionMessage;
import top.bluecraft.viewlauncher.network.ClientboundCardSelectionPacket;
import top.bluecraft.viewlauncher.network.SlotTakeMessage;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

@Mod(CombatDepot.MODID)
public class CombatDepot {

    public static final String MODID = "compatdepot";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel PACKET_HANDLER = NetworkRegistry.newSimpleChannel(new ResourceLocation(MODID, MODID), () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals);

    private static int messageID = 0;
    public static final RegistryObject<CreativeModeTab> EXAMPLE_TAB = CREATIVE_MODE_TABS.register("bcp_tab", () -> CreativeModeTab.builder().withTabsBefore(CreativeModeTabs.COMBAT).icon(ItemRegistration.GENERAL_TERMINAL.get()::getDefaultInstance).displayItems((parameters, output) -> {
        output.accept(ItemRegistration.GENERAL_TERMINAL.get());
    }).build());

    public CombatDepot() {
        @SuppressWarnings({"removal"})
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        bus.addListener(this::commonSetup);
        ItemRegistration.REGISTRATION.register(bus);
        MenuRegistration.REGISTRATION.register(bus);
        CREATIVE_MODE_TABS.register(bus);
        MinecraftForge.EVENT_BUS.register(this);
        bus.addListener(this::addCreative);
        bus.addListener(CombatDepot::onGatherData);
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            CombatDepot.addNetworkMessage(ClientboundCardSelectionPacket.class, ClientboundCardSelectionPacket::encode, ClientboundCardSelectionPacket::decode, ClientboundCardSelectionPacket.Handler::handle);
        });
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("HELLO FROM COMMON SETUP");
        LOGGER.info("DIRT BLOCK >> {}", ForgeRegistries.BLOCKS.getKey(Blocks.DIRT));
        event.enqueueWork(
                () -> {
                    CombatDepot.addNetworkMessage(SlotTakeMessage.class, SlotTakeMessage::encode, SlotTakeMessage::decode, SlotTakeMessage.Handler::handle);
                    CombatDepot.addNetworkMessage(CardSelectionMessage.class, CardSelectionMessage::encode, CardSelectionMessage::decode, CardSelectionMessage::handle);
                    CombatDepot.addNetworkMessage(AddItemToCardMessage.class, AddItemToCardMessage::encode, AddItemToCardMessage::decode, AddItemToCardMessage::handle);
                }
        );
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
    }

    public static void onGatherData(GatherDataEvent event) {
        var gen = event.getGenerator();
        var packOutput = gen.getPackOutput();
        var helper = event.getExistingFileHelper();

        gen.addProvider(event.includeClient(), new ModelProvider(packOutput, helper));
        gen.addProvider(event.includeClient(), new EnglishLanguageProvider(packOutput));
        gen.addProvider(event.includeClient(), new ChineseLanguageProvider(packOutput));
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("HELLO from server starting");
    }

    // 英文语言文件
    public static class EnglishLanguageProvider extends LanguageProvider {
        public EnglishLanguageProvider(PackOutput output) {
            super(output, CombatDepot.MODID, "en_us");
        }
        // ...

        @Override
        protected void addTranslations() {
            // 等价于 this.add("item.xiaozhong.sulfur_dust", "Sulfur Dust")
            this.add(ItemRegistration.GENERAL_TERMINAL.get(), "General Terminal");
        }
    }

    // 中文语言文件
    public static class ChineseLanguageProvider extends LanguageProvider {
        public ChineseLanguageProvider(PackOutput output) {
            super(output, CombatDepot.MODID, "zh_cn");
        }

        @Override
        protected void addTranslations() {
            // 等价于 this.add("item.xiaozhong.sulfur_dust", "硫粉")
            this.add(ItemRegistration.GENERAL_TERMINAL.get(), "通用终端");
        }
    }



    public static <T> void addNetworkMessage(Class<T> messageType, BiConsumer<T, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, T> decoder, BiConsumer<T, Supplier<NetworkEvent.Context>> messageConsumer) {
        PACKET_HANDLER.registerMessage(messageID, messageType, encoder, decoder, messageConsumer);
        messageID++;
    }
}
