package top.bluecraft.combatdepot.lang;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.common.Mod;
import top.bluecraft.combatdepot.CombatDepot;
import top.bluecraft.combatdepot.config.CardConfig;

import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = CombatDepot.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CardLanguageManager {
    private static final Map<String, Map<String, String>> cardTranslations = new HashMap<>();

    public static void loadTranslations(CardConfig config) {
        cardTranslations.clear();

        for (CardConfig.CardEntry card : config.getCards()) {
            if (card.getTranslations() != null) {
                for (Map.Entry<String, String> translation : card.getTranslations().entrySet()) {
                    cardTranslations
                            .computeIfAbsent(translation.getKey(), k -> new HashMap<>())
                            .put(getTranslationKey(card.getName()), translation.getValue());
                }
            }
        }
    }

    /**
     * 获取卡片的翻译键
     */
    public static String getTranslationKey(String cardName) {
        return "gui." + CombatDepot.MODID + "." + cardName;
    }

    /**
     * 获取卡片的翻译
     */
    @OnlyIn(Dist.CLIENT)
    public static Component getTranslation(String cardName) {
        String key = getTranslationKey(cardName);
        // 先尝试从标准语言文件获取翻译
        if (I18n.exists(key)) {
            return Component.translatable(key);
        }

        // 如果标准语言文件没有,尝试从配置获取
        String currentLanguage = Minecraft.getInstance().getLanguageManager().getSelected();
        Map<String, String> currentTranslations = cardTranslations.get(currentLanguage);
        if (currentTranslations != null && currentTranslations.containsKey(key)) {
            return Component.literal(currentTranslations.get(key));
        }

        // 如果都没有,返回卡片原始名称
        return Component.literal(cardName);
    }
}