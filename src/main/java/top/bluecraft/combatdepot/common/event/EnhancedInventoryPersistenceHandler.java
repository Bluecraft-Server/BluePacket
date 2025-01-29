package top.bluecraft.combatdepot.common.event;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.bluecraft.combatdepot.CombatDepot;
import top.bluecraft.combatdepot.api.ICardInventory;
import top.bluecraft.combatdepot.common.card.Cards;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

@Mod.EventBusSubscriber(modid = CombatDepot.MODID)
public class EnhancedInventoryPersistenceHandler {
    private static final String INVENTORIES_TAG_NAME = "CardInventories";
    private static final String GLOBAL_INVENTORY_FILENAME = "global_card_inventories.dat";

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        saveAllCardInventories(event.getServer());
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        loadGlobalCardInventories(event.getServer());
    }

    private static void saveAllCardInventories(MinecraftServer server) {
        Path worldPath = server.getWorldPath(LevelResource.PLAYER_DATA_DIR);
        File globalInventoryFile = worldPath.resolve(GLOBAL_INVENTORY_FILENAME).toFile();

        try {
            CompoundTag rootTag = new CompoundTag();
            ListTag inventoriesTag = new ListTag();

            for (ICardInventory inventory : Cards.CARD_INVENTORIES) {
                CompoundTag inventoryTag = new CompoundTag();
                inventoryTag.putString("Name", inventory.getName());
                inventoryTag.put("Data", inventory.serializeNBT());
                inventoriesTag.add(inventoryTag);
            }

            rootTag.put(INVENTORIES_TAG_NAME, inventoriesTag);
            NbtIo.write(rootTag, globalInventoryFile);
            CombatDepot.LOGGER.info("Successfully saved global card inventories");
        } catch (IOException e) {
            CombatDepot.LOGGER.error("Failed to save global card inventories", e);
        }
    }

    private static void loadGlobalCardInventories(MinecraftServer server) {
        Path worldPath = server.getWorldPath(LevelResource.PLAYER_DATA_DIR);
        File globalInventoryFile = worldPath.resolve(GLOBAL_INVENTORY_FILENAME).toFile();

        if (!globalInventoryFile.exists()) {
            return;
        }

        try {
            CompoundTag rootTag = NbtIo.read(globalInventoryFile);
            if (rootTag != null && rootTag.contains(INVENTORIES_TAG_NAME)) {
                ListTag inventoriesTag = rootTag.getList(INVENTORIES_TAG_NAME, CompoundTag.TAG_COMPOUND);

                for (int i = 0; i < inventoriesTag.size(); i++) {
                    CompoundTag inventoryTag = inventoriesTag.getCompound(i);
                    String name = inventoryTag.getString("Name");

                    // 查找匹配名称的库存
                    for (ICardInventory inventory : Cards.CARD_INVENTORIES) {
                        if (inventory.getName().equals(name)) {
                            inventory.deserializeNBT(inventoryTag.getCompound("Data"));
                            break;
                        }
                    }
                }
                CombatDepot.LOGGER.info("Successfully loaded global card inventories");
            }
        } catch (IOException e) {
            CombatDepot.LOGGER.error("Failed to load global card inventories", e);
        }
    }
}