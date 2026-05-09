package org.igye.remem3.app.controllers2.convertfillgapstotrnaslate;

import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.igye.remem3.app.Cache;
import org.igye.remem3.app.CardUtils;
import org.igye.remem3.app.Settings;
import org.igye.remem3.app.controllers.components.DirSelectorCmp;
import org.igye.remem3.app.controllers.convertfillgapstotrnaslate.NewCardKey;
import org.igye.remem3.app.dto.Card;
import org.igye.remem3.app.state.StateRenderer;
import org.igye.remem3.html.HtmlBuilder;
import org.igye.remem3.html.HtmlElem;
import org.igye.remem3.html.HtmlTag;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import static java.lang.String.format;

@RequiredArgsConstructor
public class ConvertFillGapsToTranslateRenderer extends HtmlBuilder implements StateRenderer<State> {
    public static final String PAR_DIR_TO_CONVERT_TASKS_IN = "ConvertFillGapsToTranslate_PAR_DIR_TO_CONVERT_TASKS_IN";
    public static final String PAR_GAP_SECOND_LANG = "ConvertFillGapsToTranslate_PAR_GAP_SECOND_LANG";
    public static final String PAR_SELECTED_CARD = "ConvertFillGapsToTranslate_PAR_SELECTED_CARD";
    public static final String ACT_SAVE_SELECTED_CARDS = "ConvertFillGapsToTranslate_ACT_SAVE_SELECTED_CARDS";

    public static final String ATTR_AUTO_GENERATED_FROM = "auto_generated_from";
    public static final Pattern EXISTING_CARD_KEY_PAT = Pattern.compile("^([^:]+):(.*)$", Pattern.DOTALL);

    private final Settings settings;
    private final Cache cache;
    private final CardUtils cardUtils;

    @Override
    public HtmlElem render(State st) {
        return simplePageWithTitle("Convert FillGaps to Translate",
            h4(text("Convert FillGaps to Translate")),
            rndErrors(st.getErrors()),
            form(
                rndDirSelector(st.getDir()),
                !st.getErrors().isEmpty() ? null : frag(
                    table(List.of(List.of(
                        rndGapSecondLanguage(st.getGapSecondLang()),
                        inpSubmit(ACT_SAVE_SELECTED_CARDS, "Save selected cards")
                    ))),
                    rndCards(st.getNewTranslateCards(), st.getSelectedCardKeys())
                )
            )
        );
    }

    private HtmlElem rndCards(List<Pair<NewCardKey, Card.Translate>> cards, Set<NewCardKey> selectedCardKeys) {
        return table(
            cards.stream()
                .map(p -> List.of(
                    inpCheckbox(
                        PAR_SELECTED_CARD, p.getLeft().toString(), selectedCardKeys.contains(p.getLeft())
                    ),
                    rndCard(p.getRight())
                ))
                .toList()
        );
    }

    private HtmlElem rndCard(Card.Translate card) {
        return table(List.of(
            List.of(
                text(format("%s %s", card.getLang1(), card.isExactMatch1() ? "=" : "~")),
                text(card.getText1())
            ),
            List.of(
                text(format("%s %s", card.getLang2(), card.isExactMatch2() ? "=" : "~")),
                text(card.getText2())
            ),
            List.of(
                text(""),
                pre(text(card.getNotes()))
            )
        )).attr("class", "table-single-border");
    }

    private HtmlElem rndGapSecondLanguage(String selectedLang) {
        return table(List.of(List.of(
            text("Gap second language"),
            select(PAR_GAP_SECOND_LANG, selectedLang,
                settings.getLanguages().stream()
                    .map(lang -> Pair.of(lang, text(lang)))
                    .toList()
            ).submitOnChange()
        )));
    }

    private HtmlTag rndDirSelector(DirSelectorCmp dir) {
        return table(List.of(List.of(
            text("Directory"),
            frag(dir.render())
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
}
