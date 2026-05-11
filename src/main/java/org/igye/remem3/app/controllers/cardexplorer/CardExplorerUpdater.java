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

import java.io.IOException;
import java.util.Optional;

import static org.igye.remem3.app.controllers.cardexplorer.CardExplorerRenderer.ACT_OPEN_CARD_IN_EDITOR;
import static org.igye.remem3.app.controllers.cardexplorer.CardExplorerRenderer.PAR_DIR;

@RequiredArgsConstructor
@Order(3)
public class CardExplorerUpdater implements StateUpdater<CardExplorerState> {
    private final CardExplorerConstructor constructor;
    private final Settings settings;
    private final Cache cache;

    @Override
    public CardExplorerState update(CardExplorerState st, RequestParams params) {
        st = constructor.readStateFromParams(params);
        cache.put(PAR_DIR, st.getDir().getSelectedDirectoryStr());
        if (params.hasKeyValueParam(ACT_OPEN_CARD_IN_EDITOR)) {
            actOpenCard(st, params);
        }
        return st;
    }

    @SneakyThrows
    private void actOpenCard(CardExplorerState st, RequestParams params) {
        String filePath = params.getKeyValueParam(ACT_OPEN_CARD_IN_EDITOR);
        st.getCards().stream()
            .map(Card::getFile)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .filter(file -> file.getAbsolutePath().equals(filePath))
            .forEach(file -> {
                try {
                    new ProcessBuilder(settings.getCardEditor(), file.getAbsolutePath()).start();
                } catch (IOException e) {
                    throw new Exn(e);
                }
            });
    }
}
