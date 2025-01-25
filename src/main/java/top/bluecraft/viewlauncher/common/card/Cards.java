package top.bluecraft.viewlauncher.common.card;

import top.bluecraft.viewlauncher.api.ICardInventory;
import top.bluecraft.viewlauncher.common.capability.CardInventoryCapability;

import java.util.ArrayList;
import java.util.List;

public class Cards {
    public static final List<ICardInventory> CARD_INVENTORIES = new ArrayList<>();

    static {
        CARD_INVENTORIES.add(new CardInventoryCapability(1100, "main_weapon"));
        CARD_INVENTORIES.add(new CardInventoryCapability(770, "secondary_weapon"));
        CARD_INVENTORIES.add(new CardInventoryCapability(770, "scope"));
        CARD_INVENTORIES.add(new CardInventoryCapability(770, "magazine"));
        CARD_INVENTORIES.add(new CardInventoryCapability(770, "gun_module"));
        CARD_INVENTORIES.add(new CardInventoryCapability(770, "grip"));
        CARD_INVENTORIES.add(new CardInventoryCapability(770, "stock"));
        CARD_INVENTORIES.add(new CardInventoryCapability(770, "barrel"));
        CARD_INVENTORIES.add(new CardInventoryCapability(770, "bullet"));
    }
}
