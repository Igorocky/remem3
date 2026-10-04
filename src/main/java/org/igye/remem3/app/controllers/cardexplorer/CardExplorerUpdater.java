package org.igye.remem3.app.controllers.cardexplorer;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.igye.remem3.app.Cache;
import org.igye.remem3.app.CardUtils;
import org.igye.remem3.app.Settings;
import org.igye.remem3.app.dto.Card;
import org.igye.remem3.app.state.StateUpdater;
import org.igye.remem3.utils.Exn;
import org.igye.remem3.web.RequestParams;
import org.springframework.core.annotation.Order;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;

import static org.igye.remem3.app.controllers.cardexplorer.CardExplorerRenderer.ACT_CANCEL_EDIT_PROP;
import static org.igye.remem3.app.controllers.cardexplorer.CardExplorerRenderer.ACT_DELETE_CARD;
import static org.igye.remem3.app.controllers.cardexplorer.CardExplorerRenderer.ACT_DELETE_CARD_CANCELED;
import static org.igye.remem3.app.controllers.cardexplorer.CardExplorerRenderer.ACT_DELETE_CARD_CONFIRMED;
import static org.igye.remem3.app.controllers.cardexplorer.CardExplorerRenderer.ACT_EDIT_PROP;
import static org.igye.remem3.app.controllers.cardexplorer.CardExplorerRenderer.ACT_OPEN_CARD_IN_EDITOR;
import static org.igye.remem3.app.controllers.cardexplorer.CardExplorerRenderer.ACT_REFRESH_CARD;
import static org.igye.remem3.app.controllers.cardexplorer.CardExplorerRenderer.ACT_SAVE_PROP;
import static org.igye.remem3.app.controllers.cardexplorer.CardExplorerRenderer.ACT_SET_EXACT_MATCH;
import static org.igye.remem3.app.controllers.cardexplorer.CardExplorerRenderer.ACT_SET_PRIORITY;
import static org.igye.remem3.app.controllers.cardexplorer.CardExplorerRenderer.PAR_CARD_PATH;
import static org.igye.remem3.app.controllers.cardexplorer.CardExplorerRenderer.PAR_DIR;
import static org.igye.remem3.app.controllers.cardexplorer.CardExplorerRenderer.PAR_EDITED_CARD_PATH;
import static org.igye.remem3.app.controllers.cardexplorer.CardExplorerRenderer.PAR_EDITED_PROP;
import static org.igye.remem3.app.controllers.cardexplorer.CardExplorerRenderer.PAR_EDITED_TEXT;
import static org.igye.remem3.app.controllers.cardexplorer.CardExplorerRenderer.PAR_EXACT_MATCH_SIDE;

@RequiredArgsConstructor
@Order(3)
public class CardExplorerUpdater implements StateUpdater<CardExplorerState> {
    private final CardExplorerConstructor constructor;
    private final CardExplorerRenderer renderer;
    private final Settings settings;
    private final Cache cache;
    private final CardUtils cardUtils;

    @Override
    public CardExplorerState update(CardExplorerState st, RequestParams params) {
        st = constructor.readStateFromParams(params);
        cache.put(PAR_DIR, st.getDir().getSelectedDirectoryStr());
        if (params.hasKeyValueParam(ACT_OPEN_CARD_IN_EDITOR)) {
            st = actOpenCard(st, params);
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
        if (params.hasParam(ACT_CANCEL_EDIT_PROP)) {
            st = actCancelEditProp(st, params);
        }
        if (
            params.hasKeyValueParam(ACT_DELETE_CARD)
                || params.hasKeyValueParam(ACT_DELETE_CARD_CANCELED)
                || params.hasKeyValueParam(ACT_DELETE_CARD_CONFIRMED)
        ) {
            st = actDeleteCard(st, params);
        }
        return st;
    }

    @SneakyThrows
    private CardExplorerState actOpenCard(CardExplorerState st, RequestParams params) {
        String filePath = params.getKeyValueParam(ACT_OPEN_CARD_IN_EDITOR);
        Optional<File> fileOpt = findCardByPath(st, filePath).flatMap(Card::getFile);
        fileOpt.ifPresent(file -> {
            try {
                new ProcessBuilder(settings.getCardEditor(), file.getAbsolutePath()).start();
            } catch (IOException e) {
                throw new Exn(e);
            }
        });
        return st.withScrollToId(fileOpt.map(renderer::getId));
    }

    @SneakyThrows
    private CardExplorerState actDeleteCard(CardExplorerState st, RequestParams params) {
        if (params.hasKeyValueParam(ACT_DELETE_CARD)) {
            Optional<Card> cardOpt = findCardByPath(st, params.getKeyValueParam(ACT_DELETE_CARD));
            return st.withDeleteCard(cardOpt);
        }
        if (params.hasKeyValueParam(ACT_DELETE_CARD_CANCELED)) {
            Optional<Card> cardOpt = findCardByPath(st, params.getKeyValueParam(ACT_DELETE_CARD_CANCELED));
            return st.withDeleteCard(Optional.empty())
                .withScrollToId(cardOpt.flatMap(Card::getFile).map(renderer::getId));
        }
        if (params.hasKeyValueParam(ACT_DELETE_CARD_CONFIRMED)) {
            Card cardToDelete = findCardByPath(st, params.getKeyValueParam(ACT_DELETE_CARD_CONFIRMED)).get();
            Card prevCard = null;
            for (int i = 0; i < st.getCards().size(); i++) {
                if (st.getCards().get(i) == cardToDelete) {
                    if (i != 0) {
                        prevCard = st.getCards().get(i - 1);
                    }
                    break;
                }
            }
            Files.delete(cardToDelete.getFile().get().toPath());
            return st.withDeleteCard(Optional.empty())
                .withScrollToId(Optional.ofNullable(prevCard).flatMap(Card::getFile).map(renderer::getId));
        }
        return st.withDeleteCard(Optional.empty());
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

    private CardExplorerState actCancelEditProp(CardExplorerState st, RequestParams params) {
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
