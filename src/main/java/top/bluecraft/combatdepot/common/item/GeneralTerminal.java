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
import top.bluecraft.combatdepot.api.ICardInventory;
import top.bluecraft.combatdepot.common.capability.CardInventoryCapability;
import top.bluecraft.combatdepot.common.card.Cards;

public class GeneralTerminal extends Item {
    private static final String NBT_INVENTORIES_TAG = "CardInventories";

    public GeneralTerminal() {
        super(new Properties().stacksTo(1));
    }

    @Override
    public @Nullable ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        // Check for existing inventories in NBT first
        if (nbt != null && nbt.contains(NBT_INVENTORIES_TAG)) {
            CompoundTag inventoriesTag = nbt.getCompound(NBT_INVENTORIES_TAG);

            // Try to restore from saved data
            for (ICardInventory cardInventory : Cards.CARD_INVENTORIES) {
                if (inventoriesTag.contains(cardInventory.getName())) {
                    cardInventory.deserializeNBT(inventoriesTag.getCompound(cardInventory.getName()));
                    return new TerminalCapabilityProvider(cardInventory);
                }
            }
        }

        // If no saved data, use first available inventory
        for (ICardInventory cardInventory : Cards.CARD_INVENTORIES) {
            return new TerminalCapabilityProvider(cardInventory);
        }

        // Fallback to default
        return new TerminalCapabilityProvider(new CardInventoryCapability(7700, "normal"));
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