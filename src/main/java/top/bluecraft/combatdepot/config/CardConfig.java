package top.bluecraft.combatdepot.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.loading.FMLPaths;
import top.bluecraft.combatdepot.CombatDepot;
import top.bluecraft.combatdepot.api.ICardInventory;
import top.bluecraft.combatdepot.common.capability.CardInventoryCapability;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CardConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILE = "cards.json";

    private boolean disableDefaultCards;
    private List<CardEntry> cards;

    public static class CardEntry {
        private boolean enabled;
        private String name;
        private int inventorySize;
        private String texture;
        private Map<String, String> translations;

        public boolean isEnabled() { return enabled; }
        public String getName() { return name; }
        public int getInventorySize() { return inventorySize; }
        public ResourceLocation getTexture() {
            if (texture.startsWith("file:///")) {
                // 如果是文件路径，创建基于文件的 ResourceLocation
                return new ResourceLocation("combatdepot",
                        "textures/gui/cards/" + new File(texture.substring(8)).getName());
            }
            return new ResourceLocation(texture);
        }

        // 添加 setter 方法
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public void setName(String name) { this.name = name; }
        public void setInventorySize(int inventorySize) { this.inventorySize = inventorySize; }
        public void setTexture(String texture) { this.texture = texture; }

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
            if (translations == null) {
                translations = new HashMap<>();
            }
            translations.put(lang, text);
        }
    }

    public static CardConfig load() {
        Path configDir = FMLPaths.CONFIGDIR.get().resolve(CombatDepot.MODID);
        Path configFile = configDir.resolve(CONFIG_FILE);

        // 如果配置文件不存在，创建默认配置
        if (!Files.exists(configFile)) {
            CardConfig defaultConfig = createDefaultConfig();
            save(defaultConfig);
            return defaultConfig;
        }

        // 读取配置文件
        try (Reader reader = Files.newBufferedReader(configFile)) {
            return GSON.fromJson(reader, CardConfig.class);
        } catch (Exception e) {
            CombatDepot.LOGGER.error("Failed to load card config", e);
            return createDefaultConfig();
        }
    }

    public static void save(CardConfig config) {
        Path configDir = FMLPaths.CONFIGDIR.get().resolve(CombatDepot.MODID);
        Path configFile = configDir.resolve(CONFIG_FILE);

        try {
            Files.createDirectories(configDir);
            try (Writer writer = Files.newBufferedWriter(configFile)) {
                GSON.toJson(config, writer);
            }
        } catch (Exception e) {
            CombatDepot.LOGGER.error("Failed to save card config", e);
        }
    }

    private static CardConfig createDefaultConfig() {
        CardConfig config = new CardConfig();
        config.disableDefaultCards = false;
        config.cards = new ArrayList<>();

        // 添加默认卡片配置
        config.cards.add(createCardEntry(true, "main_weapon", 1100, "combatdepot:textures/gui/main_weapon.png"));
        config.cards.add(createCardEntry(true, "secondary_weapon", 770, "combatdepot:textures/gui/secondary_weapon.png"));
        config.cards.add(createCardEntry(true, "scope", 770, "combatdepot:textures/gui/scope.png"));
        config.cards.add(createCardEntry(true, "magazine", 770, "combatdepot:textures/gui/magazine.png"));
        config.cards.add(createCardEntry(true, "gun_module", 770, "combatdepot:textures/gui/gun_module.png"));
        config.cards.add(createCardEntry(true, "grip", 770, "combatdepot:textures/gui/grip.png"));
        config.cards.add(createCardEntry(true, "stock", 770, "combatdepot:textures/gui/stock.png"));
        config.cards.add(createCardEntry(true, "barrel", 770, "combatdepot:textures/gui/barrel.png"));
        config.cards.add(createCardEntry(true, "bullet", 770, "combatdepot:textures/gui/bullet.png"));

        return config;
    }

    public static CardEntry createCardEntry(boolean enabled, String name, int size, String texture) {
        CardEntry entry = new CardEntry();
        entry.enabled = enabled;
        entry.name = name;
        entry.inventorySize = size;
        entry.texture = texture;
        return entry;
    }

    public List<ICardInventory> createInventories() {
        List<ICardInventory> inventories = new ArrayList<>();

        // 如果没有禁用默认卡片且卡片列表为空，创建默认配置
        if (!disableDefaultCards && (cards == null || cards.isEmpty())) {
            cards = createDefaultConfig().getCards();
        }

        // 创建启用的卡片的物品栏
        if (cards != null) {
            for (CardEntry card : cards) {
                if (card.enabled) {
                    inventories.add(new CardInventoryCapability(card.inventorySize, card.name));
                }
            }
        }

        return inventories;
    }

    // Getter 和 Setter 方法
    public boolean isDisableDefaultCards() {
        return disableDefaultCards;
    }

    public void setDisableDefaultCards(boolean disableDefaultCards) {
        this.disableDefaultCards = disableDefaultCards;
    }

    public List<CardEntry> getCards() {
        if (cards == null) {
            cards = new ArrayList<>();
        }
        return cards;
    }

    public void setCards(List<CardEntry> cards) {
        this.cards = cards;
    }

    // 检查卡片是否存在
    public boolean hasCard(String name) {
        return cards != null && cards.stream().anyMatch(card -> card.name.equals(name));
    }

    // 添加新卡片
    public void addCard(CardEntry card) {
        if (cards == null) {
            cards = new ArrayList<>();
        }
        if (!hasCard(card.name)) {
            cards.add(card);
        }
    }

    // 移除卡片
    public void removeCard(String name) {
        if (cards != null) {
            cards.removeIf(card -> card.name.equals(name));
        }
    }
}