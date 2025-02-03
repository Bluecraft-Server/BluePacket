package top.bluecraft.combatdepot.common.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import top.bluecraft.combatdepot.config.CardConfig;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GlobalCardStorage extends SavedData {
    // 常量定义
    private static final String STORAGE_NAME = "global_card_storage";
    private static final int DEFAULT_SLOT_SIZE = 110; // 默认槽位大小

    // 玩家卡片数据存储结构
    private final Map<UUID, PlayerCardData> playerDataMap = new HashMap<>();

    // 获取全局实例
    public static GlobalCardStorage get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                GlobalCardStorage::load,
                GlobalCardStorage::new,
                STORAGE_NAME
        );
    }

    // 加载数据
    private static GlobalCardStorage load(CompoundTag tag) {
        GlobalCardStorage storage = new GlobalCardStorage();
        if (tag.contains("PlayerData", Tag.TAG_COMPOUND)) {
            CompoundTag playerDataTag = tag.getCompound("PlayerData");

            for (String uuidStr : playerDataTag.getAllKeys()) {
                UUID uuid = UUID.fromString(uuidStr);
                CompoundTag playerTag = playerDataTag.getCompound(uuidStr);
                Map<String, ItemStackHandler> cardInventories = new HashMap<>();

                for (String cardName : playerTag.getAllKeys()) {
                    CompoundTag cardTag = playerTag.getCompound(cardName);
                    ItemStackHandler handler = new ItemStackHandler(DEFAULT_SLOT_SIZE);
                    handler.deserializeNBT(cardTag);
                    cardInventories.put(cardName, handler);
                }

                storage.playerDataMap.put(uuid, new PlayerCardData(cardInventories));
            }
        }
        return storage;
    }

    // 保存数据
    @Override
    public @NotNull CompoundTag save(CompoundTag tag) {
        CompoundTag playerDataTag = new CompoundTag();

        playerDataMap.forEach((uuid, playerData) -> {
            CompoundTag playerTag = new CompoundTag();
            playerData.cardInventories().forEach((cardName, handler) -> {
                playerTag.put(cardName, handler.serializeNBT());
            });
            playerDataTag.put(uuid.toString(), playerTag);
        });

        tag.put("PlayerData", playerDataTag);
        return tag;
    }

    // 获取或创建玩家卡片数据
    public PlayerCardData getOrCreatePlayerData(ServerPlayer player, List<CardConfig.CardEntry> cardEntries) {
        return playerDataMap.compute(player.getUUID(), (uuid, playerData) -> {
            if (playerData == null) {
                playerData = new PlayerCardData(new HashMap<>());
            }

            // 初始化缺失的卡片库存
            @org.jetbrains.annotations.Nullable PlayerCardData finalPlayerData = playerData;
            cardEntries.stream()
                    .filter(CardConfig.CardEntry::isEnabled)
                    .forEach(entry -> {
                        finalPlayerData.cardInventories().computeIfAbsent(entry.getName(),
                                k -> new ItemStackHandler(entry.getInventorySize())
                        );
                    });

            return playerData;
        });
    }

    // 更新玩家卡片库存
    public void updatePlayerInventory(ServerPlayer player, String cardName, ItemStackHandler handler) {
        PlayerCardData playerData = playerDataMap.get(player.getUUID());
        if (playerData != null) {
            playerData.cardInventories().put(cardName, handler);
            setDirty(); // 标记数据需要保存
        }
    }

    // 玩家卡片数据记录类
    public record PlayerCardData(Map<String, ItemStackHandler> cardInventories) {
        // 获取指定卡片的库存
        public ItemStackHandler getInventory(String cardName) {
            return cardInventories.get(cardName);
        }

        // 检查是否存在指定卡片的库存
        public boolean hasInventory(String cardName) {
            return cardInventories.containsKey(cardName);
        }
    }
}