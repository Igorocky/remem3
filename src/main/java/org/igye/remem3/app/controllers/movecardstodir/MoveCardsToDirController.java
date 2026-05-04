package org.igye.remem3.app.controllers.movecardstodir;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.igye.remem3.app.Cache;
import org.igye.remem3.app.CardUtils;
import org.igye.remem3.app.Settings;
import org.igye.remem3.app.controllers.components.DirSelectorCmp;
import org.igye.remem3.app.controllers.components.impl.DirSelectorCmpImpl;
import org.igye.remem3.app.dto.Card;
import org.igye.remem3.app.dto.CardType;
import org.igye.remem3.app.dto.HistRec;
import org.igye.remem3.app.dto.RepeatStrategyType;
import org.igye.remem3.app.dto.fillgaps.TextPart;
import org.igye.remem3.html.HtmlBuilder;
import org.igye.remem3.html.HtmlElem;
import org.igye.remem3.html.HtmlTag;
import org.igye.remem3.utils.Exn;
import org.igye.remem3.utils.Utils;
import org.igye.remem3.web.RequestParams;
import org.igye.remem3.web.StatefulWebController;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
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

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
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
    private final Utils utils;
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
        DirSelectorCmp dirSelectorFrom = new DirSelectorCmpImpl(settings, cache, PAR_DIR_TO_MOVE_FROM, false, params);
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
        DirSelectorCmp dirSelectorTo = new DirSelectorCmpImpl(settings, cache, PAR_DIR_TO_MOVE_TO, false, params);
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
                rndDirSelector("To directory", st.getDirMoveTo()),
                br(),
                rndRepeatStrategyTypeSelector(st),
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
            select(PAR_REPEAT_STRATEGY_TYPE, st.getRepeatStrategyType().toString(),
                Arrays.stream(RepeatStrategyType.values())
                    .map(t -> Pair.of(t.toString(), text(t.toString())))
                    .toList()
            ).submitOnChange()
        )));
    }

    private HtmlElem rndCardTypeSelector(State st) {
        return table(List.of(List.of(
            text("Card type"),
            select(PAR_CARD_TYPE_CODE, st.getCardType().getCode(),
                Arrays.stream(CardType.values())
                    .map(t -> Pair.of(t.getCode(), text(t.getDisplayName())))
                    .toList()
            ).submitOnChange()
        )));
    }

    private Pair<Long, List<String>> calcRating(Card card, RepeatStrategyType repeatStrategyType) {
        List<HistRec> hist = card.getHistory().stream()
            .filter(h -> h.getStrategy() == repeatStrategyType)
            .sorted(Comparator.comparing(HistRec::getTime).reversed())
            .takeWhile(h -> BigDecimal.ONE.equals(h.getMark()))
            .toList();
        List<String> dur = new ArrayList<>();
        int durPrecision = 1;
        if (!hist.isEmpty()) {
            dur.add(utils.durationToStr(Duration.between(hist.getFirst().getTime(), Instant.now()), durPrecision));
        }
        for (int i = 0; i < hist.size() - 1; i++) {
            dur.add(utils.durationToStr(Duration.between(hist.get(i + 1).getTime(), hist.get(i).getTime()), durPrecision));
        }
        return Pair.of((long) hist.size(), dur);
    }

    private Pair<Long, List<String>> calcRating(
        List<Card> cards,
        CardType cardType,
        String lang,
        RepeatStrategyType repeatStrategyType
    ) {
        Predicate<Card> cardFilter = makeCardFilter(cardType, lang);
        return cards.stream()
            .filter(cardFilter)
            .map(card -> Pair.of(card, calcRating(card, repeatStrategyType)))
            .min(Comparator.comparing(
                (Pair<Card, Pair<Long, List<String>>> cardPairPair) -> cardPairPair.getRight().getLeft()
            ))
            .map(Pair::getRight)
            .orElse(Pair.of(0L, List.of()));
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
            select(PAR_LANG, selectedLang,
                settings.getLanguages().stream()
                    .map(lang -> Pair.of(lang, text(lang)))
                    .toList()
            ).submitOnChange()
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
                    inpCheckbox(PAR_SELECTED_BUNDLE_ID, bundle.getId(), selectedBundleIds.contains(bundle.getId())),
                    rndBundleCards(bundle.getCards().stream().filter(cardFilter).toList()),
                    text(bundle.getRating()),
                    rndHistory(bundle.getHistory())
                ))
                .toList()
        ).attr("class", "table-single-border");
    }

    private HtmlElem rndHistory(List<String> hist) {
        List<HtmlElem> elems = new ArrayList<>();
        if (!hist.isEmpty()) {
            elems.add(h("u", text(hist.getFirst())));
        }
        for (int i = 1; i < hist.size(); i++) {
            String dur = hist.get(i);
            elems.add(dur.endsWith("d") ? h("b", text(dur)) : text(dur));
        }
        return frag(elems);
    }

    private HtmlElem rndBundleCards(List<Card> cards) {
        if (cards.isEmpty()) {
            return text("");
        } else if (cards.size() == 1) {
            return text(getCardText(cards.getFirst()));
        } else {
            return table(
                cards.stream()
                    .map(card -> List.of(text(getCardText(card))))
                    .toList()
            ).attr("class", "table-no-border");
        }
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

    @SneakyThrows
    private State actMoveSelectedCards(State st) {
        for (Bundle bundle : st.getSortedBundlesToList()) {
            if (st.getSelectedBundleIds().contains(bundle.getId())) {
                for (Card card : bundle.getCards()) {
                    File cardFile = card.getFile().get();
                    Files.move(
                        cardFile.toPath(),
                        new File(st.getDirMoveTo().getSelectedDirectory(), cardFile.getName()).toPath(),
                        ATOMIC_MOVE
                    );
                }
            }
        }
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
                    Pair<Long, List<String>> rating = calcRating(childCards, cardType, lang, repeatStrategyType);
                    return Bundle.builder()
                        .id(e.getKey())
                        .cards(childCards.stream().sorted(Comparator.comparing(this::getCardText)).toList())
                        .rating(rating.getLeft())
                        .history(rating.getRight())
                        .build();
                }
            )
            .sorted(Comparator.comparing(Bundle::getRating).reversed())
            .toList();
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
