package org.igye.remem3.app.controllers.movecardstodir;

import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.igye.remem3.app.Cache;
import org.igye.remem3.app.CardUtils;
import org.igye.remem3.app.RepeatStrategyType;
import org.igye.remem3.app.Settings;
import org.igye.remem3.app.controllers.components.DirSelectorCmp;
import org.igye.remem3.app.controllers.components.impl.DirSelectorCmpImpl;
import org.igye.remem3.app.dto.Card;
import org.igye.remem3.app.dto.CardType;
import org.igye.remem3.app.dto.HistRec;
import org.igye.remem3.app.dto.fillgaps.TextPart;
import org.igye.remem3.html.HtmlBuilder;
import org.igye.remem3.html.HtmlElem;
import org.igye.remem3.html.HtmlTag;
import org.igye.remem3.utils.Exn;
import org.igye.remem3.web.RequestParams;
import org.igye.remem3.web.StatefulWebController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.igye.remem3.app.controllers.convertfillgapstotrnaslate.ConvertFillGapsToTranslateController.ATTR_AUTO_GENERATED_FROM;

@RequiredArgsConstructor
public class MoveCardsToDirController extends HtmlBuilder
    implements StatefulWebController<State, Supplier<State>> {

    private static final String PAR_DIR_TO_MOVE_FROM = "MoveCardsToDir__PAR_DIR_TO_MOVE_FROM";
    private static final String PAR_CARD_TYPE_CODE = "MoveCardsToDir__PAR_CARD_TYPE_CODE";
    private static final String PAR_LANG = "MoveCardsToDir__PAR_LANG";
    private static final String PAR_REPEAT_STRATEGY_TYPE = "MoveCardsToDir__PAR_REPEAT_STRATEGY_TYPE";
    private static final String PAR_DIR_TO_MOVE_TO = "MoveCardsToDir__PAR_DIR_TO_MOVE_TO";
    private static final String PAR_SELECTED_BUNDLE_ID = "MoveCardsToDir__PAR_SELECTED_BUNDLE_ID";

    private static final String ACT_MOVE_SELECTED_BUNDLES = "ACT_MOVE_SELECTED_BUNDLES";

    private final Settings settings;
    private final Cache cache;
    private final CardUtils cardUtils;

    @Override
    public String getId() {
        return "move_cards_to_dir";
    }

    @Override
    public String getTitle() {
        return "Move cards to another directory";
    }

    @Override
    public State loadState(RequestParams params) {
        DirSelectorCmp dirSelectorFrom = new DirSelectorCmpImpl(settings, cache, PAR_DIR_TO_MOVE_FROM, params);
        CardType cardType = CardType.fromCode(
            params.getParam(
                PAR_CARD_TYPE_CODE,
                cache.getStr(PAR_CARD_TYPE_CODE, CardType.TRANSLATE.getCode())
            )
        );
        String lang = params.getParam(
            PAR_LANG,
            cache.getStr(PAR_LANG, settings.getLanguages().getFirst())
        );
        RepeatStrategyType repeatStrategyType = RepeatStrategyType.valueOf(
            params.getParam(
                PAR_REPEAT_STRATEGY_TYPE,
                cache.getStr(PAR_REPEAT_STRATEGY_TYPE, RepeatStrategyType.QUEUE.toString())
            )
        );
        DirSelectorCmp dirSelectorTo = new DirSelectorCmpImpl(settings, cache, PAR_DIR_TO_MOVE_TO, params);
        List<Card> allCards = cardUtils.loadAllCards(dirSelectorFrom.getSelectedDirectory());
        return State.builder()
            .params(params)
            .errors(List.of())
            .dirMoveFrom(dirSelectorFrom)
            .cardType(cardType)
            .lang(lang)
            .repeatStrategyType(repeatStrategyType)
            .dirMoveTo(dirSelectorTo)
            .sortedBundlesToList(collectBundles(allCards, cardType, lang, repeatStrategyType))
            .selectedBundleIds(
                Arrays.stream(params.getParams(PAR_SELECTED_BUNDLE_ID))
                    .collect(Collectors.toSet())
            )
            .build();
    }

    @Override
    public Optional<Supplier<State>> decodeAction(RequestParams params, State st) {
        if (!st.getErrors().isEmpty()) {
            return Optional.empty();
        }
        if (params.hasParam(ACT_MOVE_SELECTED_BUNDLES)) {
            return Optional.of(() -> actMoveSelectedCards(st));
        }
        return Optional.empty();
    }

    @Override
    public State updateState(State st, Supplier<State> action) {
        if (!st.getErrors().isEmpty()) {
            return st;
        }
        return action.get();
    }

    @Override
    public void saveState(State st) {
        cache.put(PAR_DIR_TO_MOVE_FROM, st.getDirMoveFrom().getSelectedDirectoryStr());
        cache.put(PAR_CARD_TYPE_CODE, st.getCardType().getCode());
        cache.put(PAR_LANG, st.getLang());
        cache.put(PAR_REPEAT_STRATEGY_TYPE, st.getRepeatStrategyType());
        cache.put(PAR_DIR_TO_MOVE_TO, st.getDirMoveTo().getSelectedDirectoryStr());
    }

    @Override
    public String renderState(State st) {
        return simplePageWithTitle("Move cards to another directory",
            h4(text("Move cards to another directory")),
            rndErrors(st.getErrors()),
            form(
                rndDirSelector("From directory", st.getDirMoveFrom()),
                rndCardTypeSelector(st),
                rndLanguageSelector(settings, st.getLang()),
                rndRepeatStrategyTypeSelector(st),
                rndDirSelector("To directory", st.getDirMoveTo()),
                br(),
                !st.getErrors().isEmpty() ? null : frag(
                    inpSubmit(ACT_MOVE_SELECTED_BUNDLES, "Move selected cards"),
                    rndBundles(st)
                )
            )
        ).toString();
    }

    private HtmlElem rndRepeatStrategyTypeSelector(State st) {
        return table(List.of(List.of(
            text("Repeat strategy"),
            select(PAR_REPEAT_STRATEGY_TYPE, true, st.getRepeatStrategyType().toString(),
                Arrays.stream(RepeatStrategyType.values())
                    .map(t -> Pair.of(t.toString(), text(t.toString())))
                    .toList()
            )
        )));
    }

    private HtmlElem rndCardTypeSelector(State st) {
        return table(List.of(List.of(
            text("Card type"),
            select(PAR_CARD_TYPE_CODE, true, st.getCardType().getCode(),
                Arrays.stream(CardType.values())
                    .map(t -> Pair.of(t.getCode(), text(t.getDisplayName())))
                    .toList()
            )
        )));
    }

    private long calcRating(Card card, RepeatStrategyType repeatStrategyType) {
        return card.getHistory().stream()
            .filter(h -> h.getStrategy() == repeatStrategyType)
            .sorted(Comparator.comparing(HistRec::getTime).reversed())
            .takeWhile(h -> BigDecimal.ONE.equals(h.getMark()))
            .count();
    }

    private long calcRating(
        List<Card> cards,
        CardType cardType,
        String lang,
        RepeatStrategyType repeatStrategyType
    ) {
        Predicate<Card> cardFilter = makeCardFilter(cardType, lang);
        return cards.stream()
            .filter(cardFilter)
            .map(card -> calcRating(card, repeatStrategyType))
            .min(Long::compareTo)
            .orElse(0L);
    }

    private HtmlTag rndDirSelector(String title, DirSelectorCmp dirSelector) {
        return table(List.of(List.of(
            text(title),
            frag(dirSelector.render())
        )));
    }

    private HtmlElem rndLanguageSelector(Settings settings, String selectedLang) {
        return table(List.of(List.of(
            text("Language"),
            select(PAR_LANG, true, selectedLang,
                settings.getLanguages().stream()
                    .map(lang -> Pair.of(lang, text(lang)))
                    .toList()
            )
        )));
    }

    private HtmlElem rndErrors(List<String> errors) {
        if (CollectionUtils.isEmpty(errors)) {
            return null;
        }
        return div("color:red;",
            h3(text("Error")),
            ul(errors.stream().map(msg -> pre(text(msg))).toList())
        );
    }

    private HtmlElem rndBundles(State st) {
        Predicate<Card> cardFilter = makeCardFilter(st.getCardType(), st.getLang());
        Set<String> selectedBundleIds = st.getSelectedBundleIds();
        return table(
            st.getSortedBundlesToList().stream()
                .map(bundle -> List.of(
                    inpCheckbox(PAR_SELECTED_BUNDLE_ID, "", selectedBundleIds.contains("")),
                    rndBundleCards(bundle.getCards().stream().filter(cardFilter).toList()),
                    text(bundle.getRating())
                ))
                .toList()
        ).attr("class", "table-single-border");
    }

    private HtmlElem rndBundleCards(List<Card> cards) {
        return table(
            cards.stream()
                .map(card -> List.of(text(getCardText(card))))
                .toList()
        ).attr("class", "table-single-border");
    }

    private String getCardText(Card card) {
        if (card instanceof Card.Translate tr) {
            return tr.getText1();
        } else if (card instanceof Card.FillGaps fg) {
            return fg.getText().stream()
                .map(part -> {
                    if (part instanceof TextPart.Text t) {
                        return t.getText();
                    } else if (part instanceof TextPart.Gap) {
                        return "???";
                    } else {
                        throw new Exn("Unexpected text part type %s.".formatted(part));
                    }
                })
                .collect(Collectors.joining(" "));
        } else {
            throw new Exn("Unexpected card type %s.".formatted(card));
        }
    }


    private State actMoveSelectedCards(State st) {
        return loadState(st.getParams());
    }

    private List<Bundle> collectBundles(
        List<Card> cards,
        CardType cardType,
        String lang,
        RepeatStrategyType repeatStrategyType
    ) {
        Map<String, List<Card>> parentIdToCards = cards.stream()
            .collect(Collectors.groupingBy(card -> getParentId(card), Collectors.toCollection(ArrayList::new)));
        while (merge(parentIdToCards)) {
        }
        Predicate<Card> cardFilter = makeCardFilter(cardType, lang);
        return parentIdToCards.entrySet().stream()
            .filter(e -> e.getValue().stream().anyMatch(cardFilter))
            .map(e ->
                {
                    List<Card> childCards = e.getValue();
                    return Bundle.builder()
                        .id(e.getKey())
                        .cards(childCards.stream().sorted(Comparator.comparing(this::getCardText)).toList())
                        .rating(calcRating(childCards, cardType, lang, repeatStrategyType))
                        .build();
                }
            )
            .sorted(Comparator.comparing(Bundle::getRating).reversed())
            .toList();
    }

    private int getTextLength(Card card) {
        return getCardText(card).length();
    }

    private boolean merge(Map<String, List<Card>> parentIdToCards) {
        for (Map.Entry<String, List<Card>> entry : parentIdToCards.entrySet()) {
            List<Card> childCards = entry.getValue();
            for (Card child : childCards) {
                String firstLevelParentId = entry.getKey();
                String secondLevelParentId = child.getFile().get().getName();
                if (!firstLevelParentId.equals(secondLevelParentId) && parentIdToCards.containsKey(secondLevelParentId)) {
                    childCards.addAll(parentIdToCards.get(secondLevelParentId));
                    parentIdToCards.remove(secondLevelParentId);
                    return true;
                }
            }
        }
        return false;
    }

    private String getParentId(Card card) {
        if (!card.getAttrs().containsKey(ATTR_AUTO_GENERATED_FROM)) {
            return card.getFile().get().getName();
        }
        String src = card.getAttrs().get(ATTR_AUTO_GENERATED_FROM);
        String[] parts = src.split(":");
        if (parts.length != 2) {
            throw new Exn("getBundleId: parts.length != 2");
        }
        if (StringUtils.isBlank(parts[0])) {
            throw new Exn("getBundleId: StringUtils.isBlank(parts[0])");
        }
        return parts[0];
    }

    private Predicate<Card> makeCardFilter(CardType cardType, String lang) {
        return c -> switch (cardType) {
            case FILL_GAPS -> c instanceof Card.FillGaps fg && lang.equals(fg.getLang());
            case TRANSLATE -> c instanceof Card.Translate tr && lang.equals(tr.getLang1());
        };
    }
}
