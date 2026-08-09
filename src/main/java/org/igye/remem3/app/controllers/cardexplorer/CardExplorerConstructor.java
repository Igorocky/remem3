package org.igye.remem3.app.controllers.cardexplorer;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.igye.remem3.app.Cache;
import org.igye.remem3.app.CardUtils;
import org.igye.remem3.app.Settings;
import org.igye.remem3.app.components.DirSelectorCmp;
import org.igye.remem3.app.components.PrioritySelectorCmp;
import org.igye.remem3.app.components.impl.DirSelectorCmpImpl;
import org.igye.remem3.app.components.impl.PrioritySelectorCmpImpl;
import org.igye.remem3.app.dto.Card;
import org.igye.remem3.app.dto.fillgaps.TextPart;
import org.igye.remem3.app.state.StateConstructor;
import org.igye.remem3.utils.Exn;
import org.igye.remem3.utils.NatOrdPath;
import org.igye.remem3.utils.impl.NatOrdPathImpl;
import org.igye.remem3.web.RequestParams;
import org.igye.remem3.web.StateController;
import org.igye.remem3.web.impl.RequestParamsImpl;
import org.springframework.core.annotation.Order;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import static org.igye.remem3.app.controllers.cardexplorer.CardExplorerRenderer.PAR_DIR;
import static org.igye.remem3.app.controllers.cardexplorer.CardExplorerRenderer.PAR_FILTER;
import static org.igye.remem3.app.controllers.cardexplorer.CardExplorerRenderer.PAR_PRIORITY;
import static org.igye.remem3.app.controllers.cardexplorer.CardExplorerRenderer.PAR_RECURSIVE;
import static org.igye.remem3.app.controllers.cardexplorer.CardExplorerRenderer.PAR_SORT_ASC;

@RequiredArgsConstructor
@Order(3)
public class CardExplorerConstructor implements StateConstructor<CardExplorerState> {
    public static final String PAR_INIT_PATH = "initPath";

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

    public String makeUrlWithInitPath(String initPath) {
        return "%s/%s?%s=%s".formatted(
            StateController.STATE_PATH,
            getName(),
            PAR_INIT_PATH,
            URLEncoder.encode(initPath, StandardCharsets.UTF_8)
        );
    }

    private DirSelectorCmp makeDirSelector(RequestParams params) {
        if (params.hasParam(PAR_INIT_PATH)) {
            return new DirSelectorCmpImpl(settings, cache, PAR_DIR).setPath(new File(params.getParam(PAR_INIT_PATH)));
        } else {
            return new DirSelectorCmpImpl(settings, cache, PAR_DIR).setPath(params);
        }
    }

    @SneakyThrows
    public CardExplorerState readStateFromParams(RequestParams params) {
        DirSelectorCmp dirSelectorCmp = makeDirSelector(params);
        PrioritySelectorCmp prioritySelectorCmp = new PrioritySelectorCmpImpl(PAR_PRIORITY)
            .setSelectedPriorities(params);
        boolean sortAsc = Boolean.parseBoolean(params.getParam(PAR_SORT_ASC, String.valueOf(true)));
        boolean recursive = params.hasParam(PAR_RECURSIVE);
        String filterStr = params.getParam(PAR_FILTER, "");
        Predicate<Card> filter = StringUtils.isBlank(filterStr)
            ? _ -> true
            : card -> cardMatches(card, filterStr);
        Comparator<Pair<NatOrdPath, Card>> comparator = Comparator.comparing((Pair<NatOrdPath, Card> pair) ->
            pair.getLeft()
        ).thenComparing((Pair<NatOrdPath, Card> pair) ->
            pair.getRight().getOrder()
        );
        if (!sortAsc) {
            comparator = comparator.reversed();
        }
        File dir = dirSelectorCmp.getSelectedDirectory();
        Set<Integer> selectedPriorities = prioritySelectorCmp.getSelectedPriorities();
        boolean selectAllPr = selectedPriorities.isEmpty();
        List<Card> cards = (recursive ? cardUtils.loadAllCards(dir) : cardUtils.loadCardsNonRec(dir)).stream()
            .filter(card -> selectAllPr || selectedPriorities.contains(card.getPriority()))
            .filter(filter)
            .map(card -> Pair.of(((NatOrdPath) new NatOrdPathImpl(card.getFile().get().getParentFile())), card))
            .sorted(comparator)
            .map(Pair::getRight)
            .toList();
        return CardExplorerState.builder()
            .dir(dirSelectorCmp)
            .priorities(prioritySelectorCmp)
            .sortAsc(sortAsc)
            .recursive(recursive)
            .filter(filterStr)
            .cards(cards)
            .build();
    }

    private boolean cardMatches(Card card, String filter) {
        if (StringUtils.isBlank(filter)) {
            return true;
        }
        filter = filter.trim().toLowerCase();
        return switch (card) {
            case Card.FillGaps c -> fillGapsCardMatches(c, filter);
            case Card.Translate c -> translateCardMatches(c, filter);
        };
    }

    private boolean translateCardMatches(Card.Translate card, String filter) {
        return card.getText1().toLowerCase().contains(filter)
            || card.getExample1().toLowerCase().contains(filter)
            || card.getText2().toLowerCase().contains(filter)
            || card.getExample2().toLowerCase().contains(filter)
            || card.getNotes().toLowerCase().contains(filter);
    }

    private boolean fillGapsCardMatches(Card.FillGaps card, String filter) {
        return card.getDescr().toLowerCase().contains(filter)
            || card.getNotes().toLowerCase().contains(filter)
            || card.getText().stream().anyMatch(part -> {
            if (part instanceof TextPart.Text t) {
                return t.getText().toLowerCase().contains(filter);
            } else if (part instanceof TextPart.Gap gap) {
                return gap.getAnswer().toLowerCase().contains(filter)
                    || gap.getHint().toLowerCase().contains(filter)
                    || gap.getNotes().toLowerCase().contains(filter);
            } else {
                throw new Exn("Unexpected text part type %s.".formatted(part));
            }
        });
    }

}
