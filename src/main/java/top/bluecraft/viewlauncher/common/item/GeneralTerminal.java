package top.bluecraft.viewlauncher.common.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.bluecraft.viewlauncher.ViewLauncher;
import top.bluecraft.viewlauncher.client.menu.GunViewMenu;
import top.bluecraft.viewlauncher.common.capability.CardInventoryCapability;
import top.bluecraft.viewlauncher.common.capability.ModCapabilities;
import top.bluecraft.viewlauncher.init.MenuRegistration;

public class GeneralTerminal extends Item {
    public GeneralTerminal() {
        super(new Properties().stacksTo(1));
    }

    @Override
    public @Nullable ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return GunViewMenu.CAPABILITY_PROVIDER;
    }

    @Nullable
    @Override
    public CompoundTag getShareTag(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        stack.getCapability(ModCapabilities.CARD_INVENTORY).ifPresent(inventory -> {
            tag.put("Inventory", inventory.serializeNBT());
        });

        // 如果玩家正在使用这个物品
        if (stack.getEntityRepresentation() instanceof Player player) {
            if (player.containerMenu instanceof GunViewMenu menu) {
                // 保存菜单状态
                tag.put("MenuState", menu.saveState());
            }
        }

        if (stack.getEntityRepresentation() instanceof Player player) {
            if (player.containerMenu instanceof GunViewMenu menu) {
                // 保存当前的容器状态到物品NBT
                CompoundTag menuData = new CompoundTag();
                menuData.put("ItemHandler", menu.getItemHandler().serializeNBT());
                tag.put("MenuData", menuData);
            }
        }

        return tag;
    }

    @Override
    public void readShareTag(ItemStack stack, @Nullable CompoundTag nbt) {
        if (nbt != null && nbt.contains("Inventory")) {
            stack.getCapability(ModCapabilities.CARD_INVENTORY).ifPresent(inventory -> {
                inventory.deserializeNBT(nbt.getCompound("Inventory"));
            });
        }
        if (nbt != null && nbt.contains("MenuState")) {
            // 如果玩家正在使用这个物品
            if (stack.getEntityRepresentation() instanceof Player player) {
                if (player.containerMenu instanceof GunViewMenu menu) {
                    // 读取菜单状态
                    menu.loadState(nbt.getCompound("MenuState"));
                }
            }
        }

        if (nbt != null && nbt.contains("MenuData")) {
            if (stack.getEntityRepresentation() instanceof Player player) {
                if (player.containerMenu instanceof GunViewMenu menu) {
                    // 从物品NBT加载容器状态
                    CompoundTag menuData = nbt.getCompound("MenuData");
                    menu.getItemHandler().deserializeNBT(menuData.getCompound("ItemHandler"));
                }
            }
        }
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level world, @NotNull Player entity, @NotNull InteractionHand hand) {
        ItemStack itemstack = entity.getItemInHand(hand);

        if (!world.isClientSide && entity instanceof ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(serverPlayer, new TerminalProvider(itemstack));
            ViewLauncher.LOGGER.info("[ViewLauncher] Opened server screen");
        }

        return InteractionResultHolder.success(itemstack);
    }
}