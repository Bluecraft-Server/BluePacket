package top.bluecraft.viewlauncher.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import top.bluecraft.viewlauncher.CombatDepot;
import top.bluecraft.viewlauncher.api.ICardInventory;
import top.bluecraft.viewlauncher.client.menu.GunViewMenu;
import top.bluecraft.viewlauncher.common.card.Card;
import top.bluecraft.viewlauncher.common.page.Page;

import java.util.List;
import java.util.function.Supplier;

public record CardSelectionMessage(int selectedIndex) {
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(selectedIndex);
    }

    public static CardSelectionMessage decode(FriendlyByteBuf buffer) {
        return new CardSelectionMessage(buffer.readVarInt());
    }

    public static void handle(CardSelectionMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null && player.containerMenu instanceof GunViewMenu menu) {
                // 验证索引
                List<ICardInventory> inventories = menu.getInventories();
                if (message.selectedIndex >= 0 && message.selectedIndex < inventories.size()) {
                    // 更新选择
                    menu.selectCard(message.selectedIndex);
                    // 广播更改
                    menu.broadcastChanges();
                }
            }
        });
        context.setPacketHandled(true);
    }
}