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
import top.bluecraft.combatdepot.common.data.GlobalCardStorage;
import top.bluecraft.combatdepot.common.inventory.menu.GunViewMenu;
import top.bluecraft.combatdepot.config.CardConfig;

import java.util.List;
import java.util.function.Supplier;

public record SyncCardsPacket(List<GunViewMenu.Card> cards, int cardOffset) {
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
            for (int i = 0; i < card.getInventory().size(); i++) {
                ItemStack stack = card.getInventory().get(i);
                if (!stack.isEmpty()) {
                    CompoundTag itemTag = new CompoundTag();
                    itemTag.putInt("Slot", i);
                    stack.save(itemTag);
                    itemsTag.add(itemTag);
                }
            }
            inventoryTag.put("Items", itemsTag);
            buffer.writeNbt(inventoryTag);
        });

        // 写入卡片偏移量
        buf.writeVarInt(msg.cardOffset());
    }

    public static SyncCardsPacket decode(FriendlyByteBuf buf) {
        // 读取卡片列表
        List<GunViewMenu.Card> cards = buf.readList(buffer -> {
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
                getList(inventoryTag, inventory);
            }

            // 创建卡片对象
            return new GunViewMenu.Card(
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
                    },
                    inventory
            );
        });

        // 读取卡片偏移量
        int cardOffset = buf.readVarInt();
        return new SyncCardsPacket(cards, cardOffset);
    }

    public static void getList(CompoundTag inventoryTag, NonNullList<ItemStack> inventory) {
        ListTag itemsTag = inventoryTag.getList("Items", Tag.TAG_COMPOUND);
        for (int i = 0; i < itemsTag.size(); i++) {
            CompoundTag itemTag = itemsTag.getCompound(i);
            int slot = itemTag.getInt("Slot");
            if (slot >= 0 && slot < inventory.size()) {
                inventory.set(slot, ItemStack.of(itemTag));
            }
        }
    }

    public static void handle(SyncCardsPacket message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null && player.containerMenu instanceof GunViewMenu menu) {
                // 更新客户端卡片数据
                menu.setCards(message.cards());
                // 更新卡片偏移量
                menu.setCardOffset(message.cardOffset());
                if (menu.getWorld() != null && !menu.getWorld().isClientSide()) {
                    GlobalCardStorage storage = GlobalCardStorage.get((ServerLevel) menu.getPlayer().level());
                    // 更新指定卡片的库存
                    storage.updateInventory(menu.getCurrentCard().getName(), menu.getCurrentCard().getInventory());
                    storage.setDirty(); // 确保标记为脏数据以保存
                }
            }
        });
        context.setPacketHandled(true);
    }
}