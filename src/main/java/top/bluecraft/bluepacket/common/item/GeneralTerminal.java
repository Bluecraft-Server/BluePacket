package top.bluecraft.bluepacket.common.item;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.bluecraft.bluepacket.BluePacket;
import top.bluecraft.bluepacket.api.ICardInventory;
import top.bluecraft.bluepacket.client.menu.GunViewMenu;
import top.bluecraft.bluepacket.common.capability.CardInventoryCapability;
import top.bluecraft.bluepacket.common.capability.ModCapabilities;

public class GeneralTerminal extends Item {
    private final LazyOptional<ICardInventory> holder = LazyOptional.of(() -> new CardInventoryCapability(1100));
    public GeneralTerminal() {
        super(new Properties().stacksTo(1));
    }

    @Override
    public @Nullable ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new ICapabilityProvider() {
            @Override
            public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction direction) {
                return ModCapabilities.CARD_INVENTORY.orEmpty(capability, holder);
            }
        };
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