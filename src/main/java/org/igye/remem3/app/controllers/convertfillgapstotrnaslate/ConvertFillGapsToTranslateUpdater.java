package org.igye.remem3.app.controllers.convertfillgapstotrnaslate;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.igye.remem3.app.Cache;
import org.igye.remem3.app.CardUtils;
import org.igye.remem3.app.state.StateUpdater;
import org.igye.remem3.web.RequestParams;

import java.io.File;
import java.time.Instant;
import java.util.Optional;

import static org.igye.remem3.app.controllers.convertfillgapstotrnaslate.ConvertFillGapsToTranslateRenderer.ACT_SAVE_SELECTED_CARDS;
import static org.igye.remem3.app.controllers.convertfillgapstotrnaslate.ConvertFillGapsToTranslateRenderer.PAR_DIR_TO_CONVERT_TASKS_IN;

@RequiredArgsConstructor
public class ConvertFillGapsToTranslateUpdater implements StateUpdater<State> {

    private final Cache cache;
    private final CardUtils cardUtils;
    private final ConvertFillGapsToTranslateConstructor constructor;

    @Override
    public State update(State st, RequestParams params) {
        if (!st.getErrors().isEmpty()) {
            return st;
        }
        st = constructor.readStateFromParams(params);
        if (params.hasParam(ACT_SAVE_SELECTED_CARDS)) {
            actSaveSelectedCards(st);
            cache.put(PAR_DIR_TO_CONVERT_TASKS_IN, st.getDir().getSelectedDirectoryStr());
            return constructor.readStateFromParams(params);
        }
        return st;
    }

    private void actSaveSelectedCards(State st) {
        st.getNewTranslateCards().stream()
            .filter(p -> st.getSelectedCardKeys().contains(p.getLeft()))
            .map(Pair::getRight)
            .forEach(card -> {
                card.setCreatedAt(Optional.of(Instant.now()));
                cardUtils.saveCard(
                    new File(card.getFile().get().getParentFile(), cardUtils.makeFileNameForCard(card)),
                    card
                );
            });
    }
}
