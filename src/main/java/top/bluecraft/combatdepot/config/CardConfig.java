package top.bluecraft.combatdepot.config;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.loading.FMLPaths;
import top.bluecraft.combatdepot.CombatDepot;
import top.bluecraft.combatdepot.api.ICard;
import top.bluecraft.combatdepot.common.data.GlobalCardStorage;
import top.bluecraft.combatdepot.common.inventory.Card;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static top.bluecraft.combatdepot.CombatDepot.CONFIGDIR;


public class CardConfig {
    public static String CONFIG_FILE = "cards.json";

    public boolean disableDefaultCards;
    public List<CardEntry> entries;

    // 加载配置
    public static CardConfig load() {
        File configFile = new File(CONFIGDIR, CONFIG_FILE);

        if (!configFile.exists()) {
            CardConfig defaultConfig = createDefaultConfig();
            save(defaultConfig);
            return defaultConfig;
        }

        try (JsonReader jsonReader = new JsonReader(new FileReader(configFile))) {
            if (jsonReader.toString().isEmpty()) {
                CardConfig defaultConfig = createDefaultConfig();
                save(defaultConfig);
                return defaultConfig;
            }
            return new Gson().fromJson(jsonReader, CardConfig.class);
        } catch (Exception e) {
            CombatDepot.LOGGER.error("Failed to load card config", e);
            return createDefaultConfig();
        }
    }

    // 保存配置
    public static void save(CardConfig config) {
        Path configDir = FMLPaths.CONFIGDIR.get().resolve(CombatDepot.MODID);
        File configFile = new File(CONFIGDIR, CONFIG_FILE);

        try {
            Files.createDirectories(configDir);
            try (FileWriter writer = new FileWriter(configFile)) {
                new Gson().toJson(config, writer);
                CombatDepot.LOGGER.info("配置已成功保存到: {}", configFile.getAbsolutePath());
            } catch (Exception e) {
                CombatDepot.LOGGER.error("保存配置文件时发生错误: ", e);
            }
        } catch (Exception e) {
            CombatDepot.LOGGER.error("创建配置目录时发生错误: ", e);
        }
    }

    public static CardConfig createDefaultConfig() {
        CardConfig config = new CardConfig();
        config.disableDefaultCards = false;
        config.entries = new ArrayList<>();

        // 默认卡片
        config.entries.add(createCardEntry(true, "main_weapon", 1100, "combatdepot:textures/gui/cards/main_weapon.png"));
        config.entries.add(createCardEntry(true, "secondary_weapon", 770, "combatdepot:textures/gui/cards/secondary_weapon.png"));
        config.entries.add(createCardEntry(true, "scope", 770, "combatdepot:textures/gui/cards/scope.png"));
        config.entries.add(createCardEntry(true, "magazine", 770, "combatdepot:textures/gui/cards/magazine.png"));
        config.entries.add(createCardEntry(true, "gun_module", 770, "combatdepot:textures/gui/cards/gun_module.png"));
        config.entries.add(createCardEntry(true, "grip", 770, "combatdepot:textures/gui/cards/grip.png"));
        config.entries.add(createCardEntry(true, "stock", 770, "combatdepot:textures/gui/cards/stock.png"));
        config.entries.add(createCardEntry(true, "barrel", 770, "combatdepot:textures/gui/cards/barrel.png"));
        config.entries.add(createCardEntry(true, "bullet", 770, "combatdepot:textures/gui/cards/bullet.png"));

        return config;
    }

    // 创建卡片配置项
    public static CardEntry createCardEntry(boolean enabled, String name, int size, String texture) {
        CardEntry entry = new CardEntry();
        entry.enabled = enabled;
        entry.name = name;
        entry.inventorySize = size;
        entry.texture = texture;
        entry.slotPerPage = 110;
        return entry;
    }

    public List<ICard> createCards() {
        List<ICard> cards = new ArrayList<>();
        for (CardEntry entry : entries) {
            if (entry.isEnabled()) {
                NonNullList<ItemStack> inventory = entry.getInventory();
                if (inventory == null) {
                    inventory = NonNullList.withSize(entry.getInventorySize(), ItemStack.EMPTY);
                }
                cards.add(new Card(entry, inventory));
            }
        }
        return cards;
    }

    // Getters and Setters
    public boolean isDisableDefaultCards() {
        return disableDefaultCards;
    }

    public void setDisableDefaultCards(boolean disableDefaultCards) {
        this.disableDefaultCards = disableDefaultCards;
    }

    public List<CardEntry> getEntries() {
        if (entries == null) {
            entries = new ArrayList<>();
        }
        return entries;
    }

    public void setEntries(List<CardEntry> entries) {
        this.entries = entries;
    }

    // 检查卡片是否存在
    public boolean hasCard(String name) {
        return entries != null && entries.stream().anyMatch(card -> card.name.equals(name));
    }

    // 添加新卡片
    public void addCard(CardEntry card) {
        if (entries == null) {
            entries = new ArrayList<>();
        }
        if (!hasCard(card.name)) {
            entries.add(card);
        }
    }

    // 移除卡片
    public void removeCard(String name) {
        if (entries != null) {
            entries.removeIf(card -> card.name.equals(name));
        }
    }

    public void loadFromGlobalStorage(GlobalCardStorage storage) {
        for (CardEntry entry : entries) {
            if (entry.isEnabled()) {
                // 从全局存储获取该卡片
                Card storedCard = storage.getInventory(entry.getName());
                NonNullList<ItemStack> inventory = storedCard.getInventory();

                // 如果物品栏大小与配置不符，调整大小
                if (inventory.size() != entry.getInventorySize()) {
                    NonNullList<ItemStack> newInventory = NonNullList.withSize(
                            entry.getInventorySize(),
                            ItemStack.EMPTY
                    );

                    // 复制现有数据
                    for (int i = 0; i < Math.min(inventory.size(), entry.getInventorySize()); i++) {
                        newInventory.set(i, inventory.get(i));
                    }

                    // 创建新的Card实例并保持原有的限制数据
                    Card newCard = new Card(entry, newInventory);

                    // 复制取出限制和剩余次数数据
                    for (int i = 0; i < Math.min(inventory.size(), entry.getInventorySize()); i++) {
                        int limit = storedCard.getExtractionLimit(i);
                        if (limit > 0) {
                            newCard.setExtractionLimit(i, limit);
                            newCard.setRemainingCount(i, storedCard.getRemainingCount(i));
                        }
                    }

                    // 更新存储
                    storage.updateInventory(entry.getName(), newCard);
                    entry.setInventory(newInventory);
                } else {
                    entry.setInventory(inventory);
                }
            }
        }
    }

    public static class CardEntry {
        public boolean enabled;
        public String name;
        public int inventorySize;
        public int slotPerPage;
        public String texture;
        public Map<String, String> translations;
        public NonNullList<ItemStack> inventory;


        public ResourceLocation getTexture() {
            if (texture == null) {
                // 提供默认纹理以防null
                return new ResourceLocation(CombatDepot.MODID, "textures/gui/cards/default.png");
            }
            if (texture.contains(":")) {
                String[] parts = texture.split(":", 2);
                return new ResourceLocation(parts[0], parts[1]);
            }
            return new ResourceLocation(CombatDepot.MODID, texture);
        }

        public void setTexture(String texture) {
            this.texture = texture;
        }

        // Getters
        public boolean isEnabled() {
            return enabled;
        }

        // Setters
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getInventorySize() {
            return inventorySize;
        }

        public void setInventorySize(int inventorySize) {
            this.inventorySize = inventorySize;
        }

        public int getSlotPerPage() {
            return slotPerPage;
        }

        public Map<String, String> getTranslations() {
            if (translations == null) {
                translations = new HashMap<>();
            }
            return translations;
        }

        public void setTranslations(Map<String, String> translations) {
            this.translations = translations;
        }

        public void addTranslation(String lang, String text) {
            getTranslations().put(lang, text);
        }

        public NonNullList<ItemStack> getInventory() {
            if (this.inventory == null) {
                this.inventory = NonNullList.withSize(this.inventorySize, ItemStack.EMPTY);
            }
            return this.inventory;
        }

        public void setInventory(NonNullList<ItemStack> inventory) {
            if (inventory == null) {
                CombatDepot.LOGGER.error("Attempted to set null inventory for card: {}", name);
                return;
            }

            // 如果传入的inventory大小与设定的inventorySize不一致，进行调整
            if (inventory.size() != this.inventorySize) {
                NonNullList<ItemStack> newInventory = NonNullList.withSize(this.inventorySize, ItemStack.EMPTY);
                // 复制数据，确保不超出范围
                for (int i = 0; i < Math.min(inventory.size(), this.inventorySize); i++) {
                    newInventory.set(i, inventory.get(i));
                }
                this.inventory = newInventory;
                CombatDepot.LOGGER.debug("Adjusted inventory size from {} to {} for card: {}",
                        inventory.size(), this.inventorySize, name);
            } else {
                this.inventory = inventory;
            }
        }
    }
}