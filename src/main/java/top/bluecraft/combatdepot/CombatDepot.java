package top.bluecraft.combatdepot;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
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
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;
import top.bluecraft.combatdepot.client.modelprovider.ModelProvider;
import top.bluecraft.combatdepot.config.AutoCardConfig;
import top.bluecraft.combatdepot.config.CardTextureLoader;
import top.bluecraft.combatdepot.config.ConfigManager;
import top.bluecraft.combatdepot.init.ItemRegistration;
import top.bluecraft.combatdepot.init.MenuRegistration;
import top.bluecraft.combatdepot.lang.CardLanguageManager;
import top.bluecraft.combatdepot.network.AddItemToCardMessage;
import top.bluecraft.combatdepot.network.SyncCardsPacket;
import top.bluecraft.combatdepot.network.UpdateSlotMessage;

import java.io.File;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

@Mod(CombatDepot.MODID)
public class CombatDepot {

    public static final File CONFIGDIR = new File("./config/combatdepot");
    public static final File CONFIG_FILE = new File(CONFIGDIR, "config.properties");
    public static final String MODID = "combatdepot";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final RegistryObject<CreativeModeTab> EXAMPLE_TAB = CREATIVE_MODE_TABS.register("combat_depot_tab", () -> CreativeModeTab.builder().withTabsBefore(CreativeModeTabs.COMBAT).icon(ItemRegistration.GENERAL_TERMINAL.get()::getDefaultInstance).displayItems((parameters, output) -> {
        output.accept(ItemRegistration.GENERAL_TERMINAL.get());
    }).title(Component.translatable("tab.combatdepot.creativemodetab")).build());
    public static final ConfigManager configmanager = new ConfigManager();
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel PACKET_HANDLER = NetworkRegistry.newSimpleChannel(new ResourceLocation(MODID, MODID), () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals);
    private static int messageID = 0;

    public CombatDepot() {
        @SuppressWarnings({"removal"})
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();

        // 添加本地化系统的初始化
        MinecraftForge.EVENT_BUS.register(CardLanguageManager.class);
        bus.addListener(this::commonSetup);
        bus.addListener(this::clientSetup);
        ItemRegistration.REGISTRATION.register(bus);
        MenuRegistration.REGISTRATION.register(bus);
        CREATIVE_MODE_TABS.register(bus);
        MinecraftForge.EVENT_BUS.register(this);
        bus.addListener(this::addCreative);
        bus.addListener(CombatDepot::onGatherData);
    }

    public static void onGatherData(GatherDataEvent event) {
        var gen = event.getGenerator();
        var packOutput = gen.getPackOutput();
        var helper = event.getExistingFileHelper();

        gen.addProvider(event.includeClient(), new ModelProvider(packOutput, helper));
        gen.addProvider(event.includeClient(), new EnglishLanguageProvider(packOutput));
        gen.addProvider(event.includeClient(), new ChineseLanguageProvider(packOutput));
    }

    public static <T> void addNetworkMessage(Class<T> messageType, BiConsumer<T, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, T> decoder, BiConsumer<T, Supplier<NetworkEvent.Context>> messageConsumer) {
        PACKET_HANDLER.registerMessage(messageID, messageType, encoder, decoder, messageConsumer);
        messageID++;
    }

    private void clientSetup(FMLClientSetupEvent event) {
        LOGGER.info("HELLO FROM CLIENT SETUP");
        event.enqueueWork(() -> {
        });
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("HELLO FROM COMMON SETUP");
        configmanager.load();
        event.enqueueWork(
                () -> {
                    CombatDepot.addNetworkMessage(AddItemToCardMessage.class, AddItemToCardMessage::encode, AddItemToCardMessage::decode, AddItemToCardMessage::handle);
                    CombatDepot.addNetworkMessage(SyncCardsPacket.class, SyncCardsPacket::encode, SyncCardsPacket::decode, SyncCardsPacket::handle);
                    CombatDepot.addNetworkMessage(UpdateSlotMessage.class, UpdateSlotMessage::encode, UpdateSlotMessage::decode, UpdateSlotMessage::handle);
                    CardTextureLoader.initializeTextureDirectory();
                    AutoCardConfig.updateConfigWithNewTextures();
                }
        );
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("HELLO from server starting");
    }

    public ConfigManager getConfigmanager() {
        return configmanager;
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
}
