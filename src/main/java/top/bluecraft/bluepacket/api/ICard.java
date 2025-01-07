package top.bluecraft.bluepacket.api;

import net.minecraft.resources.ResourceLocation;
import top.bluecraft.bluepacket.common.card.Card;
import top.bluecraft.bluepacket.common.page.Page;

public interface ICard {
    Page getPage(int pageIndex);
    int getTotalPages();
    void switchToPage();
    ResourceLocation resourceLocation();
    String name();
}
