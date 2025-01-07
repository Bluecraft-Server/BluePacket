package top.bluecraft.bluepacket.common.card;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CardUtil {
    public static List<Card> createCards(Card... cards) {
        return new ArrayList<Card>(Arrays.asList(cards));
    }
}
