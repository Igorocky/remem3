package org.igye.remem3.app.controllers.cardexplorer;

import org.igye.remem3.app.dto.Card;

import java.util.function.BiConsumer;
import java.util.function.Function;

public enum EditableProp {
    TRANSLATE_TEXT1(Card.Translate.class, Card.Translate::getText1, Card.Translate::setText1);

    private final Class<? extends Card> cardType;
    private final Function<Card, String> getter;
    private final BiConsumer<Card, String> setter;

    <C extends Card> EditableProp(Class<C> cardType, Function<C, String> getter, BiConsumer<C, String> setter) {
        this.cardType = cardType;
        this.getter = card -> getter.apply(cardType.cast(card));
        this.setter = (card, value) -> setter.accept(cardType.cast(card), value);
    }

    public boolean isApplicableTo(Card card) {
        return cardType.isInstance(card);
    }

    public String get(Card card) {
        return getter.apply(card);
    }

    public void set(Card card, String value) {
        setter.accept(card, value);
    }
}
