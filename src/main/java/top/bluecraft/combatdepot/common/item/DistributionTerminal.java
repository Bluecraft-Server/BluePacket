package top.bluecraft.combatdepot.common.item;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import top.bluecraft.combatdepot.CombatDepot;

public class DistributionTerminal extends Item {
    public DistributionTerminal() {
        super(new Properties().stacksTo(1));
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level world, @NotNull Player entity, @NotNull InteractionHand hand) {
        ItemStack itemstack = entity.getItemInHand(hand);

        if (!world.isClientSide && entity instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new DistributionTerminalProvider());
            CombatDepot.LOGGER.info("[CombatDepot] Opened distribution screen");
        }

        return InteractionResultHolder.success(itemstack);
    }
}
