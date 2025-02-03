package top.bluecraft.combatdepot.network;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.network.NetworkEvent;
import top.bluecraft.combatdepot.CombatDepot;
import top.bluecraft.combatdepot.client.screen.GunViewScreen;
import top.bluecraft.combatdepot.common.inventory.menu.GunViewMenu;
import top.bluecraft.combatdepot.config.CardConfig;

import java.util.List;
import java.util.function.Supplier;

public record SyncCardsPacket(List<GunViewMenu.Card> cards, int cardOffset) {
    public static void encode(SyncCardsPacket msg, FriendlyByteBuf buf) {
        buf.writeCollection(msg.cards(), (buffer, card) -> {
            buffer.writeUtf(card.getName());
            buffer.writeVarInt(card.getInventory().getSlots());
            buffer.writeVarInt(card.getSlotsPerPage());
            CompoundTag inventoryTag = card.getInventory().serializeNBT();
            buffer.writeNbt(inventoryTag);
        });
        buf.writeVarInt(msg.cardOffset()); // 同步偏移量
    }

    public static SyncCardsPacket decode(FriendlyByteBuf buf) {
        List<GunViewMenu.Card> cards = buf.readList(buffer -> {
            String name = buffer.readUtf();
            int inventorySize = buffer.readVarInt();
            int slotsPerPage = buffer.readVarInt();
            ItemStackHandler inventory = new ItemStackHandler(inventorySize);
            CompoundTag tag = buffer.readNbt();
            if (tag != null) {
                inventory.deserializeNBT(tag);
            }
            return new GunViewMenu.Card(
                    new CardConfig.CardEntry() {
                        @Override public ResourceLocation getTexture() { return new ResourceLocation(CombatDepot.MODID, "textures/gui/cards/" + name + ".png"); }
                        @Override public String getName() { return name; }
                        @Override public int getInventorySize() { return inventorySize; }
                    },
                    inventory
            );
        });
        int cardOffset = buf.readVarInt(); // 读取偏移量
        return new SyncCardsPacket(cards, cardOffset);
    }

    public static void handle(SyncCardsPacket message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null && player.containerMenu instanceof GunViewMenu menu) {
                menu.setCards(message.cards); // 更新客户端卡片数据
                menu.setCardOffset(message.cardOffset); // 更新卡片偏移量
            }
        });
        context.setPacketHandled(true);
    }
}