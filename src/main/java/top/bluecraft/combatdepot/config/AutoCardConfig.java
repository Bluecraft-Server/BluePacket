package top.bluecraft.combatdepot.config;

import net.minecraft.resources.ResourceLocation;
import top.bluecraft.combatdepot.CombatDepot;

import java.util.List;

public class AutoCardConfig {
    /**
     * 为新发现的材质自动生成卡片配置
     */
    public static void updateConfigWithNewTextures() {
        // 初始化材质目录
        CardTextureLoader.initializeTextureDirectory();

        // 复制材质到资源目录
        CardTextureLoader.copyTexturesToResources();

        // 加载当前配置
        CardConfig config = CardConfig.load();

        // 获取所有可用材质
        List<ResourceLocation> availableTextures = CardTextureLoader.getAvailableTextures();

        boolean configChanged = false;

        // 检查每个材质，如果在配置中不存在，则添加新的卡片配置
        for (ResourceLocation texture : availableTextures) {
            String cardName = texture.getPath()
                    .substring(texture.getPath().lastIndexOf('/') + 1);

            // 检查这个卡片是否已经存在于配置中
            boolean exists = config.getEntries().stream()
                    .anyMatch(card -> card.getName().equals(cardName));

            if (!exists) {
                // 创建新的卡片配置
                CardConfig.CardEntry newCard = new CardConfig.CardEntry();
                newCard.setEnabled(true);
                newCard.setName(cardName);
                newCard.setInventorySize(770); // 默认大小
                newCard.setTexture(texture.getNamespace() + ":" + texture.getPath());

                // 添加到配置中
                config.getEntries().add(newCard);
                configChanged = true;

                CombatDepot.LOGGER.info("Added new card configuration for texture: {}", cardName);
            }
        }

        // 如果有改变，保存配置
        if (configChanged) {
            CardConfig.save(config);
        }
    }
}
