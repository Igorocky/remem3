package org.igye.remem3.app.controllers.cardexplorer;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.igye.remem3.app.Cache;
import org.igye.remem3.app.Settings;
import org.igye.remem3.app.dto.Card;
import org.igye.remem3.app.state.StateUpdater;
import org.igye.remem3.utils.Exn;
import org.igye.remem3.web.RequestParams;
import org.springframework.core.annotation.Order;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import static org.igye.remem3.app.controllers.cardexplorer.CardExplorerRenderer.ACT_OPEN_CARD_IN_EDITOR;
import static org.igye.remem3.app.controllers.cardexplorer.CardExplorerRenderer.ACT_REFRESH_CARD;
import static org.igye.remem3.app.controllers.cardexplorer.CardExplorerRenderer.ACT_SET_PRIORITY;
import static org.igye.remem3.app.controllers.cardexplorer.CardExplorerRenderer.PAR_CARD_PATH;
import static org.igye.remem3.app.controllers.cardexplorer.CardExplorerRenderer.PAR_DIR;

@RequiredArgsConstructor
@Order(3)
public class CardExplorerUpdater implements StateUpdater<CardExplorerState> {
    private final CardExplorerConstructor constructor;
    private final CardExplorerRenderer renderer;
    private final Settings settings;
    private final Cache cache;

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
        return st;
    }

    @SneakyThrows
    private CardExplorerState actOpenCard(CardExplorerState st, RequestParams params) {
        String filePath = params.getKeyValueParam(ACT_OPEN_CARD_IN_EDITOR);
        Optional<File> fileOpt = st.getCards().stream()
            .map(Card::getFile)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .filter(file -> file.getAbsolutePath().equals(filePath))
            .findFirst();
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
    private CardExplorerState actRefreshCard(CardExplorerState st, RequestParams params) {
        String filePath = params.getKeyValueParam(ACT_REFRESH_CARD);
        Optional<File> fileOpt = st.getCards().stream()
            .map(Card::getFile)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .filter(file -> file.getAbsolutePath().equals(filePath))
            .findFirst();
        return st.withScrollToId(fileOpt.map(renderer::getId));
    }

    @SneakyThrows
    private CardExplorerState actChangePriorityForCard(CardExplorerState st, RequestParams params) {
        String filePath = params.getParam(PAR_CARD_PATH, "");
        Optional<Card> cardOpt = st.getCards().stream()
            .filter(card -> card.getFile().map(file -> filePath.equals(file.getAbsolutePath())).orElse(false))
            .findFirst();
        cardOpt.ifPresent(card -> {
            System.out.println("Setting new priority %s for card %s".formatted(
                params.getParam(ACT_SET_PRIORITY),
                filePath
            ));
        });
        return st.withScrollToId(cardOpt.flatMap(Card::getFile).map(renderer::getId));
    }
}
