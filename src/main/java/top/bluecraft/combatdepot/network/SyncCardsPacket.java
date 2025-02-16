package top.bluecraft.combatdepot.network;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import top.bluecraft.combatdepot.CombatDepot;
import top.bluecraft.combatdepot.api.ICard;
import top.bluecraft.combatdepot.common.inventory.Card;
import top.bluecraft.combatdepot.common.inventory.menu.CombatDepotMenu;
import top.bluecraft.combatdepot.config.CardConfig;

import java.util.List;
import java.util.function.Supplier;

public record SyncCardsPacket(List<ICard> cards, int cardOffset) {
    public static void encode(SyncCardsPacket msg, FriendlyByteBuf buf) {
        // 写入卡片列表
        buf.writeCollection(msg.cards(), (buffer, card) -> {
            // 写入卡片基本信息
            buffer.writeUtf(card.getName());
            buffer.writeVarInt(card.getInventory().size());
            buffer.writeVarInt(card.getSlotsPerPage());

            // 写入物品数据和限制数据
            CompoundTag cardTag = new CompoundTag();

            // 保存物品数据
            ListTag itemsList = new ListTag();
            NonNullList<ItemStack> inventory = card.getInventory();
            for (int i = 0; i < inventory.size(); i++) {
                ItemStack stack = inventory.get(i);
                if (!stack.isEmpty()) {
                    CompoundTag slotTag = new CompoundTag();
                    slotTag.putInt("Slot", i);
                    stack.save(slotTag);
                    itemsList.add(slotTag);
                }
            }
            cardTag.put("Items", itemsList);

            // 如果是Card实例，保存提取限制数据
            if (card instanceof Card actualCard) {
                buffer.writeNbt(actualCard.serializeNBT());
            }

            buffer.writeNbt(cardTag);
        });

        // 写入卡片偏移量
        buf.writeVarInt(msg.cardOffset());
    }

    public static SyncCardsPacket decode(FriendlyByteBuf buf) {
        // 读取卡片列表
        List<ICard> cards = buf.readList(buffer -> {
            // 读取基本信息
            String name = buffer.readUtf();
            int inventorySize = buffer.readVarInt();
            int slotsPerPage = buffer.readVarInt();

            // 读取NBT数据
            CompoundTag cardTag = buffer.readNbt();
            NonNullList<ItemStack> inventory = NonNullList.withSize(inventorySize, ItemStack.EMPTY);

            // 读取物品数据
            if (cardTag != null && cardTag.contains("Items")) {
                ListTag itemsList = cardTag.getList("Items", Tag.TAG_COMPOUND);
                for (int i = 0; i < itemsList.size(); i++) {
                    CompoundTag slotTag = itemsList.getCompound(i);
                    int slot = slotTag.getInt("Slot");
                    if (slot >= 0 && slot < inventory.size()) {
                        inventory.set(slot, ItemStack.of(slotTag));
                    }
                }
            }

            // 创建卡片配置
            CardConfig.CardEntry entry = new CardConfig.CardEntry() {
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
            };

            // 创建卡片实例
            Card card = new Card(entry, inventory);

            CompoundTag extractionData = buffer.readNbt();
            if (extractionData != null) {
                card.deserializeNBT(extractionData);
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
                    menu.selectCard(currentSelectedIndex);
                    menu.setPage(currentPage);
                }
            }
        });
        context.setPacketHandled(true);
    }
}