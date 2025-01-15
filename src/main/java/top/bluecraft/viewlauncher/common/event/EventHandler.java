package top.bluecraft.viewlauncher.common.event;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.GameShuttingDownEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.bluecraft.viewlauncher.ViewLauncher;
import top.bluecraft.viewlauncher.api.ICardInventory;
import top.bluecraft.viewlauncher.client.menu.GunViewMenu;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

@Mod.EventBusSubscriber(modid = ViewLauncher.MODID)
public class EventHandler {
    private static final String INVENTORIES_TAG_NAME = "CardInventories";

    @SubscribeEvent
    public static void onWorldSave(LevelEvent.Save event) {
        // 当世界保存时保存数据
        if (event.getLevel() instanceof ServerLevel && event.getLevel().getServer() != null) {
            saveCardInventories(event.getLevel().getServer());
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        saveCardInventories(event.getServer());
    }

    @SubscribeEvent
    public static void onGameStopping(GameShuttingDownEvent event) {
        GunViewMenu.saveInventories();
    }

    @SubscribeEvent
    public static void onWorldLoad(LevelEvent.Load event) {
        // 当世界加载时加载数据
        if (event.getLevel() instanceof ServerLevel && event.getLevel().getServer() != null) {
            loadCardInventories(event.getLevel().getServer());
        }
    }

    public static void saveCardInventories(MinecraftServer server) {
        // 获取服务器保存的数据文件夹
        Path savedDataPath = server.getWorldPath(LevelResource.ROOT);
        File inventoriesFile = savedDataPath.resolve("card_inventories.dat").toFile();

        try {
            // 创建NBT复合标签来保存所有卡片库存
            CompoundTag rootTag = new CompoundTag();
            CompoundTag inventoriesTag = new CompoundTag();

            // 序列化所有卡片库存
            GunViewMenu.cardInventories.forEach((name, inv) -> {
                inventoriesTag.put(name, inv.serializeNBT());
            });

            // 将序列化的库存添加到根标签
            rootTag.put(INVENTORIES_TAG_NAME, inventoriesTag);

            // 使用NbtIo写入文件
            NbtIo.write(rootTag, inventoriesFile);
        } catch (IOException e) {
            // 记录错误
            ViewLauncher.LOGGER.error("Failed to save card inventories", e);
        }
    }

    public static void loadCardInventories(MinecraftServer server) {
        // 获取服务器保存的数据文件夹
        Path savedDataPath = server.getWorldPath(LevelResource.ROOT);
        File inventoriesFile = savedDataPath.resolve("card_inventories.dat").toFile();

        if (!inventoriesFile.exists()) {
            return; // 如果文件不存在，则不加载
        }

        try {
            // 读取NBT文件
            CompoundTag rootTag = NbtIo.read(inventoriesFile);

            if (rootTag != null && rootTag.contains(INVENTORIES_TAG_NAME)) {
                CompoundTag inventoriesTag = rootTag.getCompound(INVENTORIES_TAG_NAME);

                // 反序列化所有卡片库存
                for (String key : inventoriesTag.getAllKeys()) {
                    if (GunViewMenu.cardInventories.containsKey(key)) {
                        ICardInventory inventory = GunViewMenu.cardInventories.get(key);
                        inventory.deserializeNBT(inventoriesTag.getCompound(key));
                    }
                }
            }
        } catch (IOException e) {
            // 记录错误
            ViewLauncher.LOGGER.error("Failed to load card inventories", e);
        }
    }
}