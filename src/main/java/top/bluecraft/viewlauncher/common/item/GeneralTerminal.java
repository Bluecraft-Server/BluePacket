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
import top.bluecraft.viewlauncher.CombatDepot;
import top.bluecraft.viewlauncher.api.ICardInventory;
import top.bluecraft.viewlauncher.client.menu.GunViewMenu;
import top.bluecraft.viewlauncher.common.capability.CardInventoryCapability;
import top.bluecraft.viewlauncher.common.capability.ModCapabilities;
import top.bluecraft.viewlauncher.common.card.Cards;

public class GeneralTerminal extends Item {
    public GeneralTerminal() {
        super(new Properties().stacksTo(1));
    }

    @Override
    public @Nullable ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        for (ICardInventory inventory: Cards.CARD_INVENTORIES) {
            return new TerminalCapabilityProvider(inventory);
        }
        return new TerminalCapabilityProvider(new CardInventoryCapability(7700, "normal"));
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level world, @NotNull Player entity, @NotNull InteractionHand hand) {
        ItemStack itemstack = entity.getItemInHand(hand);

        if (!world.isClientSide && entity instanceof ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(serverPlayer, new TerminalProvider(itemstack));
            CombatDepot.LOGGER.info("[CombatDepot] Opened server screen");
        }

        return InteractionResultHolder.success(itemstack);
    }
}