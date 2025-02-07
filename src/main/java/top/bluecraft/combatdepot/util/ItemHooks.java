package top.bluecraft.combatdepot.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;

public class ItemHooks {
    /**
     * 通过命名空间查找并获取Item实例
     *
     * @param modId    mod的命名空间 (例如 "minecraft")
     * @param itemPath 物品的路径 (例如 "diamond_sword")
     * @return Item实例，如果未找到则返回null
     */
    @Nullable
    public static Item getItem(String modId, String itemPath) {
        ResourceLocation registryName = new ResourceLocation(modId, itemPath);
        return ForgeRegistries.ITEMS.getValue(registryName);
    }

    /**
     * 通过完整的注册名查找并获取Item实例
     *
     * @param fullRegistryName 完整的注册名 (例如 "minecraft:diamond_sword")
     * @return Item实例，如果未找到则返回null
     */
    @Nullable
    public static Item getItem(String fullRegistryName) {
        ResourceLocation registryName = new ResourceLocation(fullRegistryName);
        return ForgeRegistries.ITEMS.getValue(registryName);
    }

    /**
     * 通过命名空间查找并创建ItemStack
     *
     * @param modId    mod的命名空间
     * @param itemPath 物品的路径
     * @param count    物品数量
     * @return ItemStack实例，如果未找到物品则返回空堆叠
     */
    public static ItemStack createItemStack(String modId, String itemPath, int count) {
        Item item = getItem(modId, itemPath);
        if (item == null) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(item, count);
    }

    /**
     * 检查物品是否在注册表中存在
     *
     * @param modId    mod的命名空间
     * @param itemPath 物品的路径
     * @return 如果物品存在则返回true
     */
    public static boolean exists(String modId, String itemPath) {
        ResourceLocation registryName = new ResourceLocation(modId, itemPath);
        return ForgeRegistries.ITEMS.containsKey(registryName);
    }

    /**
     * 安全地获取Item实例，如果未找到则抛出异常
     *
     * @param modId    mod的命名空间
     * @param itemPath 物品的路径
     * @return Item实例
     * @throws IllegalArgumentException 如果物品未找到
     */
    public static Item getItemOrThrow(String modId, String itemPath) {
        Item item = getItem(modId, itemPath);
        if (item == null) {
            throw new IllegalArgumentException(
                    String.format("Item not found: %s:%s", modId, itemPath)
            );
        }
        return item;
    }
}
