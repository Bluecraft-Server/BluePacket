package top.bluecraft.viewlauncher.client.menu;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import top.bluecraft.viewlauncher.CombatDepot;

import java.io.File;
import java.io.IOException;

public class PersistentItemHandler extends ItemStackHandler {
    private final int startIndex;
    private final int endIndex;
    private final GunViewMenu menu;  // 用于调用broadcastChanges

    public PersistentItemHandler(int size, int startIndex, int endIndex, GunViewMenu menu) {
        super(size);
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.menu = menu;
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return slot >= startIndex && slot < endIndex + 1;
    }

    @Override
    protected void onContentsChanged(int slot) {
        super.onContentsChanged(slot);
        if (slot >= startIndex && slot <= endIndex) {
            // 保存数据
            saveData();
            // 广播更改
            menu.broadcastChanges();
        }
    }

    public void saveData() {
        try {
            CompoundTag tag = new CompoundTag();
            tag.put("ItemHandler", serializeNBT());

            File configFile = new File(FMLPaths.CONFIGDIR.get().toFile(), "gun_view_items.nbt");
            NbtIo.write(tag, configFile);
        } catch (IOException e) {
            CombatDepot.LOGGER.error("Failed to save item handler data", e);
        }
    }

    public void loadData() {
        try {
            File configFile = new File(FMLPaths.CONFIGDIR.get().toFile(), "gun_view_items.nbt");
            if (configFile.exists()) {
                CompoundTag tag = NbtIo.read(configFile);
                if (tag != null && tag.contains("ItemHandler")) {
                    deserializeNBT(tag.getCompound("ItemHandler"));
                }
            }
        } catch (IOException e) {
            CombatDepot.LOGGER.error("Failed to load item handler data", e);
        }
    }
}