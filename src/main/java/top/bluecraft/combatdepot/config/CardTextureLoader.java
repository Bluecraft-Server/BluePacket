package top.bluecraft.combatdepot.config;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.loading.FMLPaths;
import top.bluecraft.combatdepot.CombatDepot;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class CardTextureLoader {
    private static final String TEXTURES_DIR = "card_textures";
    private static final String RESOURCE_PATH = "textures/gui/cards/";

    /**
     * 初始化材质目录
     */
    public static void initializeTextureDirectory() {
        Path texturesPath = getTexturesPath();
        if (!Files.exists(texturesPath)) {
            try {
                Files.createDirectories(texturesPath);
                // 创建一个说明文件
                Path readmePath = texturesPath.resolve("readme.txt");
                Files.writeString(readmePath,
                        """
                                将卡片材质放在此文件夹中
                                支持的格式: PNG
                                建议尺寸: 32x16 像素
                                文件名将作为卡片的标识符
                                Place the card textures in this folder
                                Supported formats: PNG
                                Recommended dimensions: 32x16 pixels
                                The file name will be used as the card's identifier"""
                );
            } catch (IOException e) {
                CombatDepot.LOGGER.error("Failed to create textures directory", e);
            }
        }
    }

    /**
     * 获取材质目录路径
     */
    public static Path getTexturesPath() {
        return FMLPaths.CONFIGDIR.get().resolve(CombatDepot.MODID).resolve(TEXTURES_DIR);
    }

    /**
     * 复制材质到模组资源目录
     */
    public static void copyTexturesToResources() {
        Path texturesPath = getTexturesPath();
        if (!Files.exists(texturesPath)) {
            return;
        }

        File[] textureFiles = texturesPath.toFile().listFiles((dir, name) ->
                name.toLowerCase().endsWith(".png"));

        if (textureFiles == null) return;

        for (File textureFile : textureFiles) {
            try {
                // 验证图片文件
                ImageIO.read(textureFile);

                // 构建目标路径
                Path resourceDir = FMLPaths.GAMEDIR.get()
                        .resolve("resources")
                        .resolve(CombatDepot.MODID)
                        .resolve(RESOURCE_PATH);

                // 确保目标目录存在
                Files.createDirectories(resourceDir);

                // 复制文件
                Path targetPath = resourceDir.resolve(textureFile.getName());
                Files.copy(textureFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);

                CombatDepot.LOGGER.info("Copied texture: {}", textureFile.getName());
            } catch (IOException e) {
                CombatDepot.LOGGER.error("Failed to process texture: {}", textureFile.getName(), e);
            }
        }
    }

    /**
     * 获取所有可用的材质
     */
    public static List<ResourceLocation> getAvailableTextures() {
        List<ResourceLocation> textures = new ArrayList<>();
        Path texturesPath = getTexturesPath();

        if (!Files.exists(texturesPath)) {
            return textures;
        }

        File[] textureFiles = texturesPath.toFile().listFiles((dir, name) ->
                name.toLowerCase().endsWith(".png"));

        if (textureFiles == null) return textures;

        for (File textureFile : textureFiles) {
            String name = textureFile.getName();
            // 移除.png后缀
            name = name.substring(0, name.length() - 4);
            // 创建ResourceLocation
            textures.add(new ResourceLocation(CombatDepot.MODID,
                    RESOURCE_PATH + name));
        }

        return textures;
    }

    /**
     * 检查材质是否存在
     */
    public static boolean textureExists(String textureName) {
        Path texturePath = getTexturesPath().resolve(textureName + ".png");
        return Files.exists(texturePath);
    }
}