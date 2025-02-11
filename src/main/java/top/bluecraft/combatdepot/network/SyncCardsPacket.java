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

            // 将 NonnullList<ItemStack> 转换为 CompoundTag
            CompoundTag inventoryTag = new CompoundTag();
            ListTag itemsTag = new ListTag();
            NonNullList<ItemStack> inventory = card.getInventory();
            GlobalCardStorage.saveTags(inventory, inventoryTag, itemsTag);
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

            // 读取库存数据
            CompoundTag inventoryTag = buffer.readNbt();
            NonNullList<ItemStack> inventory = NonNullList.withSize(inventorySize, ItemStack.EMPTY);
            if (inventoryTag != null) {
                ListTag itemsTag = inventoryTag.getList("Items", Tag.TAG_COMPOUND);
                for (int i = 0; i < itemsTag.size(); i++) {
                    CompoundTag itemTag = itemsTag.getCompound(i);
                    int slot = itemTag.getInt("Slot");
                    if (slot >= 0 && slot < inventory.size()) {
                        inventory.set(slot, ItemStack.of(itemTag));
                    }
                }
            }

            // 创建卡片对象
            return new Card(
                    new CardConfig.CardEntry() {
                        @Override
                        public ResourceLocation getTexture() {
                            return new ResourceLocation(CombatDepot.MODID, "textures/gui/cards/" + name + ".png");
                        }

                        @Override
                        public String getName() {
                            return name;
                        }

                        @Override
                        public int getInventorySize() {
                            return inventorySize;
                        }

                        @Override
                        public int getSlotPerPage() {
                            return slotsPerPage;
                        }
                    },
                    inventory
            );
        });

        // 读取卡片偏移量
        int cardOffset = buf.readVarInt();
        return new SyncCardsPacket(cards, cardOffset);
    }

    public static void handle(SyncCardsPacket message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            LocalPlayer player = Minecraft.getInstance().player;
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