package org.igye.remem3.app.controllers.cardexplorer;

import org.igye.remem3.app.CardUtils;
import org.igye.remem3.app.dto.Card;

import java.util.function.BiConsumer;
import java.util.function.Function;

public enum EditableProp {
    TRANSLATE_TEXT1(Card.Translate.class, Card.Translate::getText1, Card.Translate::setText1),
    TRANSLATE_EXAMPLE1(Card.Translate.class, Card.Translate::getExample1, Card.Translate::setExample1),
    TRANSLATE_TEXT2(Card.Translate.class, Card.Translate::getText2, Card.Translate::setText2),
    TRANSLATE_EXAMPLE2(Card.Translate.class, Card.Translate::getExample2, Card.Translate::setExample2),
    TRANSLATE_NOTES(Card.Translate.class, Card.Translate::getNotes, Card.Translate::setNotes),
    FILL_GAPS_TEXT(
        Card.FillGaps.class,
        (cardUtils, card) -> cardUtils.textToString(card.getText()),
        (cardUtils, card, value) -> card.setText(cardUtils.parseText(value))
    ),
    FILL_GAPS_NOTES(Card.FillGaps.class, Card.FillGaps::getNotes, Card.FillGaps::setNotes);

    private interface Getter<C> {
        String get(CardUtils cardUtils, C card);
    }

    private interface Setter<C> {
        void set(CardUtils cardUtils, C card, String value);
    }

    private final Class<? extends Card> cardType;
    private final Getter<Card> getter;
    private final Setter<Card> setter;

    <C extends Card> EditableProp(Class<C> cardType, Getter<C> getter, Setter<C> setter) {
        this.cardType = cardType;
        this.getter = (cardUtils, card) -> getter.get(cardUtils, cardType.cast(card));
        this.setter = (cardUtils, card, value) -> setter.set(cardUtils, cardType.cast(card), value);
    }

    <C extends Card> EditableProp(Class<C> cardType, Function<C, String> getter, BiConsumer<C, String> setter) {
        this(cardType, (_, card) -> getter.apply(card), (_, card, value) -> setter.accept(card, value));
    }

    public boolean isApplicableTo(Card card) {
        return cardType.isInstance(card);
    }

    public String get(CardUtils cardUtils, Card card) {
        return getter.get(cardUtils, card);
    }

    public void set(CardUtils cardUtils, Card card, String value) {
        setter.set(cardUtils, card, value);
    }
}
