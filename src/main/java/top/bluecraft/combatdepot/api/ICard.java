package top.bluecraft.combatdepot.api;

import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public interface ICard {
    // 获取卡牌名称
    String getName();

    // 获取卡牌库存
    NonNullList<ItemStack> getInventory();

    // 设置卡牌库存
    void setInventory(NonNullList<ItemStack> inventory);

    // 获取指定页面的物品
    List<ItemStack> getPageItems(int page);

    // 获取总页数
    int getTotalPages();

    // 获取每页的槽位数
    int getSlotsPerPage();

    // 获取卡牌纹理
    ResourceLocation getTexture();
}