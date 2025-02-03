package top.bluecraft.combatdepot.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.items.ItemStackHandler;
import top.bluecraft.combatdepot.CombatDepot;
import top.bluecraft.combatdepot.common.data.GlobalCardStorage;
import top.bluecraft.combatdepot.common.inventory.menu.GunViewMenu;

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



        public ResourceLocation getTexture() {
            if (texture.startsWith("file:///")) {
                return new ResourceLocation("combatdepot", "textures/gui/cards/" + new File(texture.substring(8)).getName());
            }
            return new ResourceLocation(texture);
        }

        // Getters
        public boolean isEnabled() { return enabled; }
        public String getName() { return name; }
        public int getInventorySize() { return inventorySize; }
        public Map<String, String> getTranslations() {
            if (translations == null) {
                translations = new HashMap<>();
            }
            return translations;
        }

        // Setters
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public void setName(String name) { this.name = name; }
        public void setInventorySize(int inventorySize) { this.inventorySize = inventorySize; }
        public void setTexture(String texture) { this.texture = texture; }
        public void setTranslations(Map<String, String> translations) { this.translations = translations; }
        public void addTranslation(String lang, String text) {
            getTranslations().put(lang, text);
        }
    }

    // 加载配置
    public static CardConfig load() {
        Path configDir = FMLPaths.CONFIGDIR.get().resolve(CombatDepot.MODID);
        Path configFile = configDir.resolve(CONFIG_FILE);

        if (!Files.exists(configFile)) {
            CardConfig defaultConfig = createDefaultConfig();
            save(defaultConfig);
            return defaultConfig;
        }

        try (Reader reader = Files.newBufferedReader(configFile)) {
            return GSON.fromJson(reader, CardConfig.class);
        } catch (Exception e) {
            CombatDepot.LOGGER.error("Failed to load card config", e);
            return createDefaultConfig();
        }
    }

    // 保存配置
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

        // 默认卡片
        config.cards.add(createCardEntry(true, "main_weapon", 1100, "combatdepot:textures/gui/cards/main_weapon.png"));
        config.cards.add(createCardEntry(true, "secondary_weapon", 770, "combatdepot:textures/gui/cards/secondary_weapon.png"));
        config.cards.add(createCardEntry(true, "scope", 770, "combatdepot:textures/gui/cards/scope.png"));
        config.cards.add(createCardEntry(true, "magazine", 770, "combatdepot:textures/gui/cards/magazine.png"));
        config.cards.add(createCardEntry(true, "gun_module", 770, "combatdepot:textures/gui/cards/gun_module.png"));
        config.cards.add(createCardEntry(true, "grip", 770, "combatdepot:textures/gui/cards/grip.png"));
        config.cards.add(createCardEntry(true, "stock", 770, "combatdepot:textures/gui/cards/stock.png"));
        config.cards.add(createCardEntry(true, "barrel", 770, "combatdepot:textures/gui/cards/barrel.png"));
        config.cards.add(createCardEntry(true, "bullet", 770, "combatdepot:textures/gui/cards/bullet.png"));

        return config;
    }

    // 创建卡片配置项
    public static CardEntry createCardEntry(boolean enabled, String name, int size, String texture) {
        CardEntry entry = new CardEntry();
        entry.enabled = enabled;
        entry.name = name;
        entry.inventorySize = size;
        entry.texture = texture;
        return entry;
    }

    // 初始化卡片库存
    public List<GunViewMenu.Card> createCards() {
        List<GunViewMenu.Card> cardList = new ArrayList<>();

        // 确保始终使用最新配置
        List<CardEntry> effectiveCards = this.cards == null || this.cards.isEmpty()
                ? createDefaultConfig().getCards()
                : this.cards;

        for (CardEntry entry : effectiveCards) {
            if (entry.isEnabled()) {
                // 使用配置中的 inventorySize
                ItemStackHandler inventory = new ItemStackHandler(entry.getInventorySize());
                cardList.add(new GunViewMenu.Card(entry, inventory));
            }
        }
        return cardList;
    }

    // Getters and Setters
    public boolean isDisableDefaultCards() { return disableDefaultCards; }
    public void setDisableDefaultCards(boolean disableDefaultCards) { this.disableDefaultCards = disableDefaultCards; }

    public List<CardEntry> getCards() {
        if (cards == null) {
            cards = new ArrayList<>();
        }
        return cards;
    }

    public void setCards(List<CardEntry> cards) { this.cards = cards; }

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

    public void loadFromGlobalStorage(ServerPlayer player, GlobalCardStorage storage) {
        // 获取所有卡片配置
        List<CardConfig.CardEntry> cardEntries = getCards();

        // 初始化全局库存（确保所有卡片库存存在）
        storage.initializeCards(cardEntries);

        // 遍历所有卡片
        for (CardConfig.CardEntry cardEntry : cardEntries) {
            if (!cardEntry.isEnabled()) continue;

            String cardName = cardEntry.getName();
            ItemStackHandler savedInventory = storage.getInventory(cardName);

            if (savedInventory != null) {
                // 创建新的 ItemStackHandler 并复制数据
                ItemStackHandler newInventory = new ItemStackHandler(cardEntry.getInventorySize());

                // 复制已保存的物品数据
                int slots = Math.min(savedInventory.getSlots(), newInventory.getSlots());
                for (int i = 0; i < slots; i++) {
                    ItemStack stack = savedInventory.getStackInSlot(i);
                    if (!stack.isEmpty()) {
                        newInventory.setStackInSlot(i, stack.copy());
                    }
                }

                // 更新全局库存
                storage.updateInventory(cardName, newInventory);
            }
        }
    }
}