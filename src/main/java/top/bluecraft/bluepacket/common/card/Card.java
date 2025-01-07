package top.bluecraft.bluepacket.common.card;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import top.bluecraft.bluepacket.common.page.Page;

public class Card {
    private String cardId;
    private Page page;
    private int pageCount;

    public Card(String cardId) {
        this.cardId = cardId;
    }


}
