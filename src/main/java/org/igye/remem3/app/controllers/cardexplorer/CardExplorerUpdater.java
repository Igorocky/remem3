package org.igye.remem3.app.controllers.cardexplorer;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.igye.remem3.app.Cache;
import org.igye.remem3.app.CardUtils;
import org.igye.remem3.app.controllers.deletecard.State;
import org.igye.remem3.app.dto.Card;
import org.igye.remem3.app.state.StateUpdater;
import org.igye.remem3.utils.Exn;
import org.igye.remem3.web.RequestParams;
import org.springframework.core.annotation.Order;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.igye.remem3.app.controllers.cardexplorer.CardExplorerRenderer.ACT_ADD_CARD_ABOVE;
import static org.igye.remem3.app.controllers.cardexplorer.CardExplorerRenderer.ACT_ADD_CARD_BELOW;
import static org.igye.remem3.app.controllers.cardexplorer.CardExplorerRenderer.ACT_CANCEL_EDIT_CARD;
import static org.igye.remem3.app.controllers.cardexplorer.CardExplorerRenderer.ACT_CANCEL_EDIT_PROP;
import static org.igye.remem3.app.controllers.cardexplorer.CardExplorerRenderer.ACT_DELETE_CARD;
import static org.igye.remem3.app.controllers.cardexplorer.CardExplorerRenderer.ACT_EDIT_CARD;
import static org.igye.remem3.app.controllers.cardexplorer.CardExplorerRenderer.ACT_EDIT_PROP;
import static org.igye.remem3.app.controllers.cardexplorer.CardExplorerRenderer.ACT_REFRESH_CARD;
import static org.igye.remem3.app.controllers.cardexplorer.CardExplorerRenderer.ACT_SAVE_CARD;
import static org.igye.remem3.app.controllers.cardexplorer.CardExplorerRenderer.ACT_SAVE_PROP;
import static org.igye.remem3.app.controllers.cardexplorer.CardExplorerRenderer.ACT_SET_EXACT_MATCH;
import static org.igye.remem3.app.controllers.cardexplorer.CardExplorerRenderer.ACT_SET_PRIORITY;
import static org.igye.remem3.app.controllers.cardexplorer.CardExplorerRenderer.PAR_CARD_PATH;
import static org.igye.remem3.app.controllers.cardexplorer.CardExplorerRenderer.PAR_DIR;
import static org.igye.remem3.app.controllers.cardexplorer.CardExplorerRenderer.PAR_EDITED_CARD_PATH;
import static org.igye.remem3.app.controllers.cardexplorer.CardExplorerRenderer.PAR_EDITED_CARD_TEXT;
import static org.igye.remem3.app.controllers.cardexplorer.CardExplorerRenderer.PAR_EDITED_PROP;
import static org.igye.remem3.app.controllers.cardexplorer.CardExplorerRenderer.PAR_EDITED_TEXT;
import static org.igye.remem3.app.controllers.cardexplorer.CardExplorerRenderer.PAR_EXACT_MATCH_SIDE;

@RequiredArgsConstructor
@Order(3)
public class CardExplorerUpdater implements StateUpdater<CardExplorerState> {
    private final CardExplorerConstructor constructor;
    private final CardExplorerRenderer renderer;
    private final Cache cache;
    private final CardUtils cardUtils;

    @Override
    public Object update(CardExplorerState st, RequestParams params) {
        st = constructor.readStateFromParams(params);
        cache.put(PAR_DIR, st.getDir().getSelectedDirectoryStr());
        if (params.hasKeyValueParam(ACT_EDIT_CARD)) {
            st = actEditCard(st, params);
        }
        if (params.hasParam(ACT_SAVE_CARD)) {
            st = actSaveCard(st, params);
        }
        if (params.hasKeyValueParam(ACT_ADD_CARD_ABOVE)) {
            st = actAddCard(st, params, params.getKeyValueParam(ACT_ADD_CARD_ABOVE), false);
        }
        if (params.hasKeyValueParam(ACT_ADD_CARD_BELOW)) {
            st = actAddCard(st, params, params.getKeyValueParam(ACT_ADD_CARD_BELOW), true);
        }
        if (params.hasKeyValueParam(ACT_REFRESH_CARD)) {
            st = actRefreshCard(st, params);
        }
        if (params.hasParam(ACT_SET_PRIORITY)) {
            st = actChangePriorityForCard(st, params);
        }
        if (params.hasParam(ACT_SET_EXACT_MATCH)) {
            st = actSetExactMatchForCard(st, params);
        }
        if (params.hasParam(ACT_EDIT_PROP)) {
            st = actEditProp(st, params);
        }
        if (params.hasParam(ACT_SAVE_PROP)) {
            st = actSaveProp(st, params);
        }
        if (params.hasParam(ACT_CANCEL_EDIT_PROP) || params.hasParam(ACT_CANCEL_EDIT_CARD)) {
            st = actCancelEdit(st, params);
        }
        if (params.hasKeyValueParam(ACT_DELETE_CARD)) {
            return actDeleteCard(st, params);
        }
        return st;
    }

    private CardExplorerState actEditCard(CardExplorerState st, RequestParams params) {
        Optional<Card> cardOpt = findCardByPath(st, params.getKeyValueParam(ACT_EDIT_CARD));
        return st
            .withCardEdit(cardOpt.map(card -> new CardEdit(
                card.getFile().get().getAbsolutePath(), cardUtils.cardToString(card), List.of()
            )))
            .withScrollToId(cardOpt.flatMap(Card::getFile).map(renderer::getId));
    }

    private CardExplorerState actSaveCard(CardExplorerState st, RequestParams params) {
        String filePath = params.getParam(PAR_EDITED_CARD_PATH, "");
        String newText = params.getParam(PAR_EDITED_CARD_TEXT, "");
        Optional<Card> cardOpt = findCardByPath(st, filePath);
        if (cardOpt.isEmpty()) {
            return st;
        }
        Card oldCard = cardOpt.get();
        File file = oldCard.getFile().get();
        st = st.withScrollToId(Optional.of(renderer.getId(file)));
        Card newCard = null;
        List<String> errors;
        try {
            newCard = cardUtils.parseCard(newText, file);
            errors = cardUtils.validateCard(newCard);
        } catch (Exception ex) {
            errors = List.of(String.valueOf(ex.getMessage()));
        }
        if (!errors.isEmpty()) {
            return st.withCardEdit(Optional.of(new CardEdit(filePath, newText, errors)));
        }
        cardUtils.saveCard(newCard);
        return st.withCards(constructor.readStateFromParams(params).getCards());
    }

    private CardExplorerState actAddCard(CardExplorerState st, RequestParams params, String filePath, boolean below) {
        Optional<Card> cardOpt = findCardByPath(st, filePath);
        if (cardOpt.isEmpty()) {
            return st;
        }
        Card existingCard = cardOpt.get();
        File dir = existingCard.getFile().get().getParentFile();
        Card newCard = switch (existingCard) {
            case Card.Translate c -> Card.Translate.builder()
                .createdAt(Optional.of(Instant.now()))
                .lang1(c.getLang1())
                .text1(c.getLang1())
                .exactMatch1(c.isExactMatch1())
                .lang2(c.getLang2())
                .text2(c.getLang2())
                .exactMatch2(c.isExactMatch2())
                .build();
            case Card.FillGaps c -> Card.FillGaps.builder()
                .createdAt(Optional.of(Instant.now()))
                .lang(c.getLang())
                .text(cardUtils.parseText("[[text]]"))
                .build();
        };
        newCard.setPriority(existingCard.getPriority());
        newCard.setOrder(below ? getOrderForCardBelow(existingCard, dir) : getOrderForCardAbove(existingCard, dir));
        File newFile = new File(dir, cardUtils.makeFileNameForCard(newCard));
        cardUtils.saveCard(newFile, newCard);
        return st
            .withCards(constructor.readStateFromParams(params).getCards())
//            .withCardEdit(Optional.of(new CardEdit(
//                newFile.getAbsolutePath(), cardUtils.cardToString(newCard), List.of()
//            )))
            .withScrollToId(Optional.of(renderer.getId(below ? existingCard.getFile().get() : newFile)));
    }

    private BigDecimal getOrderForCardBelow(Card existingCard, File dir) {
        BigDecimal existingOrder = existingCard.getOrder();
        if (existingOrder == null) {
            //null makes CardUtils.saveCard() put the new card after all the cards in the directory
            return null;
        }
        return cardUtils.loadCardsNonRec(dir).stream()
            .map(Card::getOrder)
            .filter(order -> order != null && order.compareTo(existingOrder) > 0)
            .min(BigDecimal::compareTo)
            .map(nextOrder -> existingOrder.add(nextOrder).divide(BigDecimal.TWO))
            .orElseGet(() -> existingOrder.setScale(0, RoundingMode.FLOOR).add(BigDecimal.ONE));
    }

    private BigDecimal getOrderForCardAbove(Card existingCard, File dir) {
        BigDecimal existingOrder = existingCard.getOrder();
        if (existingOrder == null) {
            //null makes CardUtils.saveCard() put the new card after all the cards in the directory
            return null;
        }
        return cardUtils.loadCardsNonRec(dir).stream()
            .map(Card::getOrder)
            .filter(order -> order != null && order.compareTo(existingOrder) < 0)
            .max(BigDecimal::compareTo)
            .map(prevOrder -> existingOrder.add(prevOrder).divide(BigDecimal.TWO))
            .orElseGet(() -> existingOrder.setScale(0, RoundingMode.CEILING).subtract(BigDecimal.ONE));
    }

    private Object actDeleteCard(CardExplorerState st, RequestParams params) {
        Optional<Card> cardOpt = findCardByPath(st, params.getKeyValueParam(ACT_DELETE_CARD));
        if (cardOpt.isEmpty()) {
            return st;
        }
        Card cardToDelete = cardOpt.get();
        return State.builder()
            .card(cardToDelete)
            .onComplete(() -> removeCard(st, cardToDelete))
            .onCancel(st.withScrollToId(cardToDelete.getFile().map(renderer::getId)))
            .build();
    }

    private CardExplorerState removeCard(CardExplorerState st, Card cardToRemove) {
        Optional<Card> prevCard = Optional.empty();
        for (Card card : st.getCards()) {
            if (card == cardToRemove) {
                break;
            }
            prevCard = Optional.of(card);
        }
        return st
            .withCards(st.getCards().stream().filter(c -> c != cardToRemove).toList())
            .withScrollToId(prevCard.flatMap(Card::getFile).map(renderer::getId));
    }

    private static Optional<Card> findCardByPath(CardExplorerState st, String filePath) {
        return st.getCards().stream()
            .filter(card -> card.getFile().map(file -> filePath.equals(file.getAbsolutePath())).orElse(false))
            .findFirst();
    }

    @SneakyThrows
    private CardExplorerState actRefreshCard(CardExplorerState st, RequestParams params) {
        String filePath = params.getKeyValueParam(ACT_REFRESH_CARD);
        Optional<File> fileOpt = findCardByPath(st, filePath).flatMap(Card::getFile);
        return st.withScrollToId(fileOpt.map(renderer::getId));
    }

    private CardExplorerState actSetExactMatchForCard(CardExplorerState st, RequestParams params) {
        Optional<Card> cardOpt = findCardByPath(st, params.getParam(PAR_CARD_PATH, ""));
        cardOpt.ifPresent(card -> {
            if (card instanceof Card.Translate translateCard) {
                boolean exactMatch = Boolean.parseBoolean(params.getParam(ACT_SET_EXACT_MATCH));
                if ("1".equals(params.getParam(PAR_EXACT_MATCH_SIDE, ""))) {
                    translateCard.setExactMatch1(exactMatch);
                } else {
                    translateCard.setExactMatch2(exactMatch);
                }
                cardUtils.saveCard(card);
            }
        });
        return st.withScrollToId(cardOpt.flatMap(Card::getFile).map(renderer::getId));
    }

    private CardExplorerState actEditProp(CardExplorerState st, RequestParams params) {
        EditableProp prop = EditableProp.valueOf(params.getParam(ACT_EDIT_PROP));
        Optional<Card> cardOpt = findCardByPath(st, params.getParam(PAR_CARD_PATH, ""))
            .filter(prop::isApplicableTo);
        return st
            .withPropEdit(cardOpt.map(card -> new CardPropEdit(
                card.getFile().get().getAbsolutePath(), prop, prop.get(cardUtils, card), List.of()
            )))
            .withScrollToId(cardOpt.flatMap(Card::getFile).map(renderer::getId));
    }

    private CardExplorerState actSaveProp(CardExplorerState st, RequestParams params) {
        String filePath = params.getParam(PAR_EDITED_CARD_PATH, "");
        EditableProp prop = EditableProp.valueOf(params.getParam(PAR_EDITED_PROP));
        String newValue = params.getParam(PAR_EDITED_TEXT, "");
        Optional<Card> cardOpt = findCardByPath(st, filePath).filter(prop::isApplicableTo);
        if (cardOpt.isEmpty()) {
            return st;
        }
        Card card = cardOpt.get();
        String oldValue = prop.get(cardUtils, card);
        List<String> errors;
        try {
            prop.set(cardUtils, card, newValue);
            errors = cardUtils.validateCard(card);
        } catch (Exn ex) {
            errors = List.of(ex.getMessage());
        }
        st = st.withScrollToId(card.getFile().map(renderer::getId));
        if (!errors.isEmpty()) {
            prop.set(cardUtils, card, oldValue);
            return st.withPropEdit(Optional.of(new CardPropEdit(filePath, prop, newValue, errors)));
        }
        cardUtils.saveCard(card);
        return st;
    }

    private CardExplorerState actCancelEdit(CardExplorerState st, RequestParams params) {
        Optional<Card> cardOpt = findCardByPath(st, params.getParam(PAR_EDITED_CARD_PATH, ""));
        return st.withScrollToId(cardOpt.flatMap(Card::getFile).map(renderer::getId));
    }

    @SneakyThrows
    private CardExplorerState actChangePriorityForCard(CardExplorerState st, RequestParams params) {
        String filePath = params.getParam(PAR_CARD_PATH, "");
        Optional<Card> cardOpt = st.getCards().stream()
            .filter(card -> card.getFile().map(file -> filePath.equals(file.getAbsolutePath())).orElse(false))
            .findFirst();
        cardOpt.ifPresent(card -> {
            card.setPriority(Integer.parseInt(params.getParam(ACT_SET_PRIORITY)));
            cardUtils.saveCard(card);
        });
        return st.withScrollToId(cardOpt.flatMap(Card::getFile).map(renderer::getId));
    }
}
