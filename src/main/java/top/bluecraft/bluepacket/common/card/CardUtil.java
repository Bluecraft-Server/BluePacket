package top.bluecraft.bluepacket.common.card;

import top.bluecraft.bluepacket.api.ICard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CardUtil{
    public static final List<ICard> CARDS = new ArrayList<>();

    public static List<ICard> createCards(ICard... cards) {
        CARDS.addAll(Arrays.asList(cards));
        return CARDS;
    }
}
