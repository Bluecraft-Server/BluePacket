package top.bluecraft.bluepacket.common;

import top.bluecraft.bluepacket.common.card.Card;

public enum CardType {
    MAIN_WEAPON(), SECONDARY_WEAPON(), EQUIPMENT(), THROWING_WEAPON(), MISC();
    private final Card card;
    CardType(Card card) {
        this.card = card;
    }

    public Card getCard() {
        return card;
    }
}
