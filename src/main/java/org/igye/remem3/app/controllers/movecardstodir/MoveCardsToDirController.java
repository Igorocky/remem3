package org.igye.remem3.app.controllers.movecardstodir;

import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
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
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class MoveCardsToDirController extends HtmlBuilder
    implements StatefulWebController<State, Supplier<State>> {

    private static final String PAR_DIR_TO_MOVE_FROM = "MoveCardsToDir__PAR_DIR_TO_MOVE_FROM";
    private static final String PAR_CARD_TYPE_CODE = "MoveCardsToDir__PAR_CARD_TYPE_CODE";
    private static final String PAR_LANG = "MoveCardsToDir__PAR_LANG";
    private static final String PAR_REPEAT_STRATEGY_TYPE = "MoveCardsToDir__PAR_REPEAT_STRATEGY_TYPE";
    private static final String PAR_DIR_TO_MOVE_TO = "MoveCardsToDir__PAR_DIR_TO_MOVE_TO";
    private static final String PAR_SELECTED_CARD_ID = "MoveCardsToDir__PAR_SELECTED_CARD_ID";

    private static final String ACT_MOVE_SELECTED_CARDS = "ACT_MOVE_SELECTED_CARDS";

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
        Comparator<Pair<Card, Long>> comparator = Comparator.comparing(Pair::getRight);
        List<Pair<Card, Long>> cardsToList = allCards.stream()
            .filter(c ->
                switch (cardType) {
                    case FILL_GAPS -> c instanceof Card.FillGaps fg && lang.equals(fg.getLang());
                    case TRANSLATE -> c instanceof Card.Translate tr && lang.equals(tr.getLang1());
                }
            )
            .map(c -> Pair.of(c, calcRating(c, lang, repeatStrategyType)))
            .sorted(comparator.reversed())
            .toList();
        Set<String> selectedCardIds = Arrays.stream(params.getParams(PAR_SELECTED_CARD_ID))
            .collect(Collectors.toSet());
        return State.builder()
            .params(params)
            .errors(List.of())
            .dirMoveFrom(dirSelectorFrom)
            .cardType(cardType)
            .lang(lang)
            .repeatStrategyType(repeatStrategyType)
            .dirMoveTo(dirSelectorTo)
            .sortedCardsToList(cardsToList)
            .selectedCardIds(selectedCardIds)
            .build();
    }

    @Override
    public Optional<Supplier<State>> decodeAction(RequestParams params, State st) {
        if (!st.getErrors().isEmpty()) {
            return Optional.empty();
        }
        if (params.hasParam(ACT_MOVE_SELECTED_CARDS)) {
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
                !st.getErrors().isEmpty() ? null : frag(
                    inpSubmit(ACT_MOVE_SELECTED_CARDS, "Move selected cards"),
                    rndCards(st.getSortedCardsToList(), st.getSelectedCardIds())
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

    private long calcRating(Card card, String lang, RepeatStrategyType repeatStrategyType) {
        return card.getHistory().stream()
            .filter(h -> h.getStrategy() == repeatStrategyType)
            .sorted(Comparator.comparing(HistRec::getTime).reversed())
            .takeWhile(h -> BigDecimal.ONE.equals(h.getMark()))
            .count();
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

    private HtmlElem rndCards(List<Pair<Card, Long>> cards, Set<String> selectedCardIds) {
        return table(
            cards.stream()
                .map(pair -> List.of(
                    inpCheckbox(
                        PAR_SELECTED_CARD_ID, "", selectedCardIds.contains("")
                    ),
                    text(getCardText(pair.getLeft())),
                    text(pair.getRight())
                ))
                .toList()
        );
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
}
