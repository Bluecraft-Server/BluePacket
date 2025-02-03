package top.bluecraft.combatdepot.common.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.bluecraft.combatdepot.CombatDepot;
import top.bluecraft.combatdepot.common.capability.GeneralTerminalInventoryCapability;

public class GeneralTerminal extends Item {
    private static final String NBT_INVENTORIES_TAG = "CardInventories";

    public GeneralTerminal() {
        super(new Properties().stacksTo(1));
    }

    @Override
    public @Nullable ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new GeneralTerminalInventoryCapability();
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level world, @NotNull Player entity, @NotNull InteractionHand hand) {
        ItemStack itemstack = entity.getItemInHand(hand);

        if (!world.isClientSide && entity instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new TerminalProvider(itemstack));
            CombatDepot.LOGGER.info("[CombatDepot] Opened server screen");
        }

        return InteractionResultHolder.success(itemstack);
    }
}