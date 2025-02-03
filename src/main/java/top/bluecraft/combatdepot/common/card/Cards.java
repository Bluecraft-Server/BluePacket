package top.bluecraft.combatdepot.common.card;

import net.minecraftforge.items.ItemStackHandler;
import top.bluecraft.combatdepot.api.ICardInventory;
import top.bluecraft.combatdepot.common.inventory.menu.GunViewMenu;
import top.bluecraft.combatdepot.config.AutoCardConfig;
import top.bluecraft.combatdepot.config.CardConfig;
import top.bluecraft.combatdepot.lang.CardLanguageManager;

import java.util.ArrayList;
import java.util.List;

public class Cards {
    public static final List<GunViewMenu.Card> CARD_INVENTORIES = new ArrayList<>();

    static {
        // 确保材质已更新
        AutoCardConfig.updateConfigWithNewTextures();
        // 加载配置和创建卡片
        CardConfig config = CardConfig.load();
        // 加载本地化
        CardLanguageManager.loadTranslations(config);
        CARD_INVENTORIES.addAll(config.createCards());
    }
}
