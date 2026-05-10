package org.igye.remem3.app.controllers2.movecardstodir;

import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.igye.remem3.app.Settings;
import org.igye.remem3.app.controllers.components.DirSelectorCmp;
import org.igye.remem3.app.dto.Card;
import org.igye.remem3.app.dto.CardType;
import org.igye.remem3.app.dto.RepeatStrategyType;
import org.igye.remem3.app.state.StateRenderer;
import org.igye.remem3.html.HtmlBuilder;
import org.igye.remem3.html.HtmlElem;
import org.igye.remem3.html.HtmlTag;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

@RequiredArgsConstructor
public class MoveCardsToDirRenderer extends HtmlBuilder implements StateRenderer<State> {
    public static final String PAR_DIR_TO_MOVE_FROM = "MoveCardsToDir_PAR_DIR_TO_MOVE_FROM";
    public static final String PAR_CARD_TYPE_CODE = "MoveCardsToDir_PAR_CARD_TYPE_CODE";
    public static final String PAR_LANG = "MoveCardsToDir_PAR_LANG";
    public static final String PAR_REPEAT_STRATEGY_TYPE = "MoveCardsToDir_PAR_REPEAT_STRATEGY_TYPE";
    public static final String PAR_DIR_TO_MOVE_TO = "MoveCardsToDir_PAR_DIR_TO_MOVE_TO";
    public static final String PAR_SELECTED_BUNDLE_ID = "MoveCardsToDir_PAR_SELECTED_BUNDLE_ID";
    public static final String ACT_MOVE_SELECTED_BUNDLES = "MoveCardsToDir_ACT_MOVE_SELECTED_BUNDLES";

    private final Settings settings;
    private final MoveCardsToDirConstructor constructor;

    @Override
    public HtmlElem render(State st) {
        return simplePageWithTitle("Move cards to another directory",
            h4(text("Move cards to another directory")),
            rndErrors(st.getErrors()),
            form(
                rndDirSelector("From directory", st.getDirMoveFrom()),
                rndCardTypeSelector(st),
                rndLanguageSelector(st.getLang()),
                rndDirSelector("To directory", st.getDirMoveTo()),
                br(),
                rndRepeatStrategyTypeSelector(st),
                br(),
                !st.getErrors().isEmpty() ? null : frag(
                    inpSubmit(ACT_MOVE_SELECTED_BUNDLES, "Move selected cards"),
                    rndBundles(st)
                )
            )
        );
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

    private HtmlTag rndDirSelector(String title, DirSelectorCmp dirSelector) {
        return table(List.of(List.of(
            text(title),
            dirSelector.render()
        )));
    }

    private HtmlElem rndLanguageSelector(String selectedLang) {
        return table(List.of(List.of(
            text("Language"),
            select(PAR_LANG, selectedLang,
                settings.getLanguages().stream()
                    .map(lang -> Pair.of(lang, text(lang)))
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

    private HtmlElem rndBundles(State st) {
        Predicate<Card> cardFilter = constructor.makeCardFilter(st.getCardType(), st.getLang());
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
            return text(constructor.getCardText(cards.getFirst()));
        } else {
            return table(
                cards.stream()
                    .map(card -> List.of(text(constructor.getCardText(card))))
                    .toList()
            ).attr("class", "table-no-border");
        }
    }
}
