package org.igye.remem3.app.controllers.validatecards;

import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.igye.remem3.app.Cache;
import org.igye.remem3.app.CardUtils;
import org.igye.remem3.app.Settings;
import org.igye.remem3.app.components.impl.DirSelectorCmpImpl;
import org.igye.remem3.app.dto.Card;
import org.igye.remem3.app.state.StateUpdater;
import org.igye.remem3.web.RequestParams;

import java.io.File;
import java.util.Comparator;
import java.util.List;

import static org.igye.remem3.app.controllers.validatecards.ValidateCardsRenderer.ACT_VALIDATE;
import static org.igye.remem3.app.controllers.validatecards.ValidateCardsRenderer.PAR_DIR;

@RequiredArgsConstructor
public class ValidateCardsUpdater implements StateUpdater<ValidateCardsState> {
    private final Settings settings;
    private final Cache cache;
    private final CardUtils cardUtils;

    @Override
    public ValidateCardsState update(ValidateCardsState st, RequestParams params) {
        if (params.hasKeyValueParam(PAR_DIR)) {
            st = st.withDir(new DirSelectorCmpImpl(settings, cache, PAR_DIR, false, params))
                .withErrors(List.of());
        }
        if (CollectionUtils.isNotEmpty(st.getErrors())) {
            return st;
        }
        if (params.hasParam(ACT_VALIDATE)) {
            return actValidateCards(st);
        }
        return st;
    }

    private ValidateCardsState actValidateCards(ValidateCardsState st) {
        File dir = st.getDir().getSelectedDirectory();
        String dirStr = dir.getAbsolutePath();
        if (!dir.exists()) {
            return st.withErrors(List.of("The specified directory doesn't exist: %s".formatted(dirStr)));
        }
        if (!dir.isDirectory()) {
            return st.withErrors(List.of("Not a directory: %s".formatted(dirStr)));
        }
        List<Card> cards = cardUtils.loadAllCards(dir);
        if (cards.isEmpty()) {
            return st.withErrors(List.of("The specified directory doesn't contains cards: %s".formatted(dirStr)));
        }
        return st.withCardsAndErrors(
            cards.stream()
                .sorted(Comparator.comparing(c -> c.getFile().get().getAbsolutePath()))
                .map(card -> Pair.of(card, cardUtils.validateCard(card)))
                .toList()
        );
    }
}
