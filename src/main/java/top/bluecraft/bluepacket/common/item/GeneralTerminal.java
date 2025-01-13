package top.bluecraft.bluepacket.common.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.bluecraft.bluepacket.BluePacket;
import top.bluecraft.bluepacket.client.menu.GunViewMenu;

public class GeneralTerminal extends Item {
    public GeneralTerminal() {
        super(new Properties().stacksTo(1));
    }

    @Override
    public @Nullable CompoundTag getShareTag(ItemStack stack) {
        CompoundTag tag = super.getShareTag(stack);
        if (tag == null) {
            tag = new CompoundTag();
        }

        // 获取或创建菜单标签
        CompoundTag menuTag = tag.getCompound("MenuData");

        // 如果玩家正在使用这个物品且打开了菜单
        if (stack.getEntityRepresentation() instanceof Player player &&
                player.containerMenu instanceof GunViewMenu menu) {
            // 保存物品栏数据
            menuTag.put("Inventories", menu.saveInventories());
        }

        tag.put("MenuData", menuTag);
        return tag;
    }

    @Override
    public void readShareTag(ItemStack stack, @Nullable CompoundTag tag) {
        super.readShareTag(stack, tag);
        if (tag != null && tag.contains("MenuData")) {
            CompoundTag menuTag = tag.getCompound("MenuData");

            // 如果玩家正在使用这个物品且打开了菜单
            if (stack.getEntityRepresentation() instanceof Player player &&
                    player.containerMenu instanceof GunViewMenu menu) {
                // 加载物品栏数据
                if (menuTag.contains("Inventories")) {
                    menu.loadInventories(menuTag.getCompound("Inventories"));
                }
            }
        }
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level world, @NotNull Player entity, @NotNull InteractionHand hand) {
        ItemStack itemstack = entity.getItemInHand(hand);

        if (!world.isClientSide && entity instanceof ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(serverPlayer, new TerminalProvider(itemstack));
            BluePacket.LOGGER.info("[BluePacket] Opened server screen");
        }

        return InteractionResultHolder.success(itemstack);
    }
}