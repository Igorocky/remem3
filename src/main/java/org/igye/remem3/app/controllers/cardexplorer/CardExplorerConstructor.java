package org.igye.remem3.app.controllers.cardexplorer;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.igye.remem3.app.Cache;
import org.igye.remem3.app.CardUtils;
import org.igye.remem3.app.Settings;
import org.igye.remem3.app.controllers.components.DirSelectorCmp;
import org.igye.remem3.app.controllers.components.impl.DirSelectorCmpImpl;
import org.igye.remem3.app.dto.Card;
import org.igye.remem3.app.state.StateConstructor;
import org.igye.remem3.web.RequestParams;
import org.igye.remem3.web.impl.RequestParamsImpl;
import org.springframework.core.annotation.Order;

import java.io.File;
import java.util.Comparator;
import java.util.List;

import static org.igye.remem3.app.controllers.cardexplorer.CardExplorerRenderer.PAR_DIR;
import static org.igye.remem3.app.controllers.cardexplorer.CardExplorerRenderer.PAR_RECURSIVE;
import static org.igye.remem3.app.controllers.cardexplorer.CardExplorerRenderer.PAR_SORT_ASC;

@RequiredArgsConstructor
@Order(3)
public class CardExplorerConstructor implements StateConstructor<CardExplorerState> {
    private final Settings settings;
    private final Cache cache;
    private final CardUtils cardUtils;

    @Override
    public String getName() {
        return "card_explorer";
    }

    @Override
    public String getDisplayName() {
        return "Card explorer";
    }

    @Override
    public CardExplorerState construct() {
        return readStateFromParams(RequestParamsImpl.empty());
    }

    @Override
    public boolean isSingleton() {
        return false;
    }

    @SneakyThrows
    public CardExplorerState readStateFromParams(RequestParams params) {
        DirSelectorCmp dirSelectorCmp = new DirSelectorCmpImpl(settings, cache, PAR_DIR, false, params);
        boolean sortAsc = Boolean.parseBoolean(params.getParam(PAR_SORT_ASC, String.valueOf(true)));
        boolean recursive = params.hasParam(PAR_RECURSIVE);
        Comparator<Card> comparator = Comparator.comparing((Card card) -> card.getFile().get().getParentFile())
            .thenComparing(Card::getOrder);
        if (!sortAsc) {
            comparator = comparator.reversed();
        }
        File dir = dirSelectorCmp.getSelectedDirectory();
        List<Card> cards = (recursive ? cardUtils.loadAllCards(dir) : cardUtils.loadCardsNonRec(dir)).stream()
            .sorted(comparator)
            .toList();
        return CardExplorerState.builder()
            .dir(dirSelectorCmp)
            .sortAsc(sortAsc)
            .recursive(recursive)
            .cards(cards)
            .build();
    }

}
