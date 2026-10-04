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
import java.util.Optional;

import static org.igye.remem3.app.controllers.cardexplorer.CardExplorerRenderer.ACT_DELETE_CARD;
import static org.igye.remem3.app.controllers.cardexplorer.CardExplorerRenderer.ACT_DELETE_CARD_CANCELED;
import static org.igye.remem3.app.controllers.cardexplorer.CardExplorerRenderer.ACT_DELETE_CARD_CONFIRMED;
import static org.igye.remem3.app.controllers.cardexplorer.CardExplorerRenderer.ACT_OPEN_CARD_IN_EDITOR;
import static org.igye.remem3.app.controllers.cardexplorer.CardExplorerRenderer.ACT_REFRESH_CARD;
import static org.igye.remem3.app.controllers.cardexplorer.CardExplorerRenderer.ACT_SET_EXACT_MATCH;
import static org.igye.remem3.app.controllers.cardexplorer.CardExplorerRenderer.ACT_SET_PRIORITY;
import static org.igye.remem3.app.controllers.cardexplorer.CardExplorerRenderer.PAR_CARD_PATH;
import static org.igye.remem3.app.controllers.cardexplorer.CardExplorerRenderer.PAR_DIR;
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
