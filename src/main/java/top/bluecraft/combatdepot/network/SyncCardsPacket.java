package top.bluecraft.combatdepot.network;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import top.bluecraft.combatdepot.CombatDepot;
import top.bluecraft.combatdepot.api.ICard;
import top.bluecraft.combatdepot.common.data.GlobalCardStorage;
import top.bluecraft.combatdepot.common.inventory.Card;
import top.bluecraft.combatdepot.common.inventory.menu.CombatDepotMenu;
import top.bluecraft.combatdepot.config.CardConfig;

import java.util.List;
import java.util.function.Supplier;

public record SyncCardsPacket(List<ICard> cards, int cardOffset) {
    public static void encode(SyncCardsPacket msg, FriendlyByteBuf buf) {
        // 写入卡片列表
        buf.writeCollection(msg.cards(), (buffer, card) -> {
            // 写入卡片名称
            buffer.writeUtf(card.getName());
            // 写入库存大小
            buffer.writeVarInt(card.getInventory().size());
            // 写入每页槽位数量
            buffer.writeVarInt(card.getSlotsPerPage());

            // 创建并写入物品数据
            CompoundTag inventoryTag = new CompoundTag();
            ListTag itemsTag = new ListTag();
            NonNullList<ItemStack> inventory = card.getInventory();
            for (int i = 0; i < inventory.size(); i++) {
                ItemStack stack = inventory.get(i);
                if (!stack.isEmpty()) {
                    CompoundTag slotTag = new CompoundTag();
                    slotTag.putInt("Slot", i);
                    stack.save(slotTag);
                    itemsTag.add(slotTag);
                }
            }
            inventoryTag.put("Items", itemsTag);

            // 如果是Card类型，写入额外数据
            if (card instanceof Card actualCard) {
                // 写入取出限制数据
                CompoundTag limitsTag = new CompoundTag();
                actualCard.getRemainingCounts().forEach((slot, count) ->
                        limitsTag.putInt(String.valueOf(slot), count));
                inventoryTag.put("RemainingCounts", limitsTag);

                // 写入剩余次数数据
                CompoundTag extractionLimitsTag = actualCard.saveExtractionLimits();
                inventoryTag.put("ExtractionLimits", extractionLimitsTag.getCompound("ExtractionLimits"));
            }

            buffer.writeNbt(inventoryTag);
        });

        // 写入卡片偏移量
        buf.writeVarInt(msg.cardOffset());
    }

    public static SyncCardsPacket decode(FriendlyByteBuf buf) {
        // 读取卡片列表
        List<ICard> cards = buf.readList(buffer -> {
            // 读取卡片名称
            String name = buffer.readUtf();
            // 读取库存大小
            int inventorySize = buffer.readVarInt();
            // 读取每页槽位数量
            int slotsPerPage = buffer.readVarInt();

            // 读取NBT数据
            CompoundTag inventoryTag = buffer.readNbt();
            NonNullList<ItemStack> inventory = NonNullList.withSize(inventorySize, ItemStack.EMPTY);

            // 读取物品数据
            if (inventoryTag != null && inventoryTag.contains("Items")) {
                ListTag itemsTag = inventoryTag.getList("Items", Tag.TAG_COMPOUND);
                for (int i = 0; i < itemsTag.size(); i++) {
                    CompoundTag slotTag = itemsTag.getCompound(i);
                    int slot = slotTag.getInt("Slot");
                    if (slot >= 0 && slot < inventory.size()) {
                        inventory.set(slot, ItemStack.of(slotTag));
                    }
                }
            }

            // 创建卡片实例
            Card card = new Card(new CardConfig.CardEntry() {
                @Override
                public String getName() {
                    return name;
                }

                @Override
                public ResourceLocation getTexture() {
                    return new ResourceLocation(CombatDepot.MODID, "textures/gui/cards/" + name + ".png");
                }

                @Override
                public int getInventorySize() {
                    return inventorySize;
                }

                @Override
                public int getSlotPerPage() {
                    return slotsPerPage;
                }
            }, inventory);

            // 读取额外数据
            if (inventoryTag != null) {
                // 读取取出限制数据
                if (inventoryTag.contains("ExtractionLimits")) {
                    CompoundTag limitTag = new CompoundTag();
                    limitTag.put("ExtractionLimits", inventoryTag.getCompound("ExtractionLimits"));
                    card.loadExtractionLimits(limitTag);
                }

                // 读取剩余次数数据
                GlobalCardStorage.readRemainingCounts(inventoryTag, card);
            }

            return card;
        });

        // 读取卡片偏移量
        int cardOffset = buf.readVarInt();
        return new SyncCardsPacket(cards, cardOffset);
    }

    public static void handle(SyncCardsPacket message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null && player.containerMenu instanceof CombatDepotMenu menu) {
                int currentSelectedIndex = menu.getSelectedCardIndex();
                int currentPage = menu.getCurrentPage();

                // 更新卡片数据
                menu.setCards(message.cards());
                menu.setCardOffset(message.cardOffset());

                // 如果当前选中的卡片仍然有效，保持选择
                if (currentSelectedIndex >= 0 && currentSelectedIndex < message.cards().size()) {
                    // 重新加载当前页面数据
                    menu.selectCard(currentSelectedIndex);
                    menu.setPage(currentPage);
                }
            }
        });
        context.setPacketHandled(true);
    }
}