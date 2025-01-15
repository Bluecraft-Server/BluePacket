package top.bluecraft.viewlauncher.common.item;

import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.bluecraft.viewlauncher.api.ICardInventory;
import top.bluecraft.viewlauncher.common.capability.ModCapabilities;
public class TerminalCapabilityProvider implements ICapabilityProvider {
    private final LazyOptional<ICardInventory> holder;
    public TerminalCapabilityProvider(ICardInventory inventory) {
        this.holder = LazyOptional.of(() -> inventory);
    }
    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction direction) {
        return ModCapabilities.CARD_INVENTORY.orEmpty(capability, holder);
    }
};