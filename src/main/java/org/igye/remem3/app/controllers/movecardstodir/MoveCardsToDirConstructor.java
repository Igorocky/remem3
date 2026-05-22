package org.igye.remem3.app.controllers.movecardstodir;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.igye.remem3.app.Cache;
import org.igye.remem3.app.CardUtils;
import org.igye.remem3.app.Settings;
import org.igye.remem3.app.components.DirSelectorCmp;
import org.igye.remem3.app.components.impl.DirSelectorCmpImpl;
import org.igye.remem3.app.dto.Card;
import org.igye.remem3.app.dto.CardType;
import org.igye.remem3.app.dto.HistRec;
import org.igye.remem3.app.dto.RepeatStrategyType;
import org.igye.remem3.app.dto.fillgaps.TextPart;
import org.igye.remem3.app.state.StateConstructor;
import org.igye.remem3.utils.Exn;
import org.igye.remem3.utils.Utils;
import org.igye.remem3.web.RequestParams;
import org.igye.remem3.web.impl.RequestParamsImpl;
import org.springframework.core.annotation.Order;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.igye.remem3.app.controllers.convertfillgapstotrnaslate.ConvertFillGapsToTranslateRenderer.ATTR_AUTO_GENERATED_FROM;
import static org.igye.remem3.app.controllers.movecardstodir.MoveCardsToDirRenderer.PAR_CARD_TYPE_CODE;
import static org.igye.remem3.app.controllers.movecardstodir.MoveCardsToDirRenderer.PAR_DIR_TO_MOVE_FROM;
import static org.igye.remem3.app.controllers.movecardstodir.MoveCardsToDirRenderer.PAR_DIR_TO_MOVE_TO;
import static org.igye.remem3.app.controllers.movecardstodir.MoveCardsToDirRenderer.PAR_LANG;
import static org.igye.remem3.app.controllers.movecardstodir.MoveCardsToDirRenderer.PAR_REPEAT_STRATEGY_TYPE;
import static org.igye.remem3.app.controllers.movecardstodir.MoveCardsToDirRenderer.PAR_SELECTED_BUNDLE_ID;

@RequiredArgsConstructor
@Order(5)
public class MoveCardsToDirConstructor implements StateConstructor<State> {
    private final Settings settings;
    private final Cache cache;
    private final Utils utils;
    private final CardUtils cardUtils;

    @Override
    public String getName() {
        return "move_cards_to_dir";
    }

    @Override
    public String getDisplayName() {
        return "Move cards to another directory";
    }

    @Override
    public State construct() {
        return readStateFromParams(RequestParamsImpl.empty());
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    public State readStateFromParams(RequestParams params) {
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

    public Predicate<Card> makeCardFilter(CardType cardType, String lang) {
        return c -> switch (cardType) {
            case FILL_GAPS -> c instanceof Card.FillGaps fg && lang.equals(fg.getLang());
            case TRANSLATE -> c instanceof Card.Translate tr && lang.equals(tr.getLang1());
        };
    }

    public String getCardText(Card card) {
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
            .map(e -> {
                List<Card> childCards = e.getValue();
                Pair<Long, List<String>> rating = calcRating(childCards, cardType, lang, repeatStrategyType);
                return Bundle.builder()
                    .id(e.getKey())
                    .cards(childCards.stream().sorted(Comparator.comparing(this::getCardText)).toList())
                    .rating(rating.getLeft())
                    .history(rating.getRight())
                    .build();
            })
            .sorted(Comparator.comparing(Bundle::getRating).reversed())
            .toList();
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
}
