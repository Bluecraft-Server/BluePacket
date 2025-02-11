package top.bluecraft.combatdepot.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.loading.FMLPaths;
import top.bluecraft.combatdepot.CombatDepot;
import top.bluecraft.combatdepot.api.ICard;
import top.bluecraft.combatdepot.common.data.GlobalCardStorage;
import top.bluecraft.combatdepot.common.inventory.Card;
import top.bluecraft.combatdepot.common.inventory.menu.CombatDepotMenu;

import java.io.File;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CardConfig {
    private static final Gson GSON = new GsonBuilder()
            .excludeFieldsWithModifiers(Modifier.TRANSIENT)
            .create();
    private static final String CONFIG_FILE = "cards.json";

    private boolean disableDefaultCards;
    private List<CardEntry> entries;

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
                // 从全局存储获取该卡片的物品栏
                NonNullList<ItemStack> inventory = storage.getInventory(entry.getName());

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

                    // 更新存储
                    storage.updateInventory(entry.getName(), newInventory);
                    entry.setInventory(newInventory);
                } else {
                    entry.setInventory(inventory);
                }
            }
        }
    }

    public static class CardEntry {
        private boolean enabled;
        private String name;
        private int inventorySize;
        private int slotPerPage;
        private String texture;
        private Map<String, String> translations;
        private NonNullList<ItemStack> inventory;


        public ResourceLocation getTexture() {
            if (texture.startsWith("file:///")) {
                return new ResourceLocation("combatdepot", "textures/gui/cards/" + new File(texture.substring(8)).getName());
            }
            return new ResourceLocation(texture);
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

        public int getSlotPerPage() {
            return slotPerPage;
        }

        public void setInventorySize(int inventorySize) {
            this.inventorySize = inventorySize;
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