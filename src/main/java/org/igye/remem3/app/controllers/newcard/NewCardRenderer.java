package org.igye.remem3.app.controllers.newcard;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.igye.remem3.app.Settings;
import org.igye.remem3.app.components.DirSelectorCmp;
import org.igye.remem3.app.controllers.cardexplorer.CardExplorerConstructor;
import org.igye.remem3.app.dto.CardType;
import org.igye.remem3.app.state.StateRenderer;
import org.igye.remem3.html.HtmlBuilder;
import org.igye.remem3.html.HtmlElem;
import org.igye.remem3.html.HtmlTag;

import java.util.Arrays;
import java.util.List;

import static java.lang.String.format;

@RequiredArgsConstructor
public class NewCardRenderer extends HtmlBuilder implements StateRenderer<NewCardState> {
    public static final String PAR_DIR_TO_SAVE_NEW_CARD_TO = "NewCard_PAR_DIR_TO_SAVE_NEW_CARD_TO";
    public static final String PAR_CARD_TYPE = "NewCard_PAR_CARD_TYPE";

    public static final String PAR_CARD_FILL_GAPS_LANG = "NewCard_PAR_CARD_FILL_GAPS_LANG";
    public static final String PAR_CARD_FILL_GAPS_TEXT = "NewCard_PAR_CARD_FILL_GAPS_TEXT";
    public static final String PAR_CARD_FILL_GAPS_NOTES = "NewCard_PAR_CARD_FILL_GAPS_NOTES";

    public static final String PAR_CARD_TRANSLATE_LANG_1 = "NewCard_PAR_CARD_TRANSLATE_LANG_1";
    public static final String PAR_CARD_TRANSLATE_EXACT_MATCH_1 = "NewCard_PAR_CARD_TRANSLATE_EXACT_MATCH_1";
    public static final String PAR_CARD_TRANSLATE_TEXT_1 = "NewCard_PAR_CARD_TRANSLATE_TEXT_1";
    public static final String PAR_CARD_TRANSLATE_EXAMPLE_1 = "NewCard_PAR_CARD_TRANSLATE_EXAMPLE_1";
    public static final String PAR_CARD_TRANSLATE_LANG_2 = "NewCard_PAR_CARD_TRANSLATE_LANG_2";
    public static final String PAR_CARD_TRANSLATE_EXACT_MATCH_2 = "NewCard_PAR_CARD_TRANSLATE_EXACT_MATCH_2";
    public static final String PAR_CARD_TRANSLATE_TEXT_2 = "NewCard_PAR_CARD_TRANSLATE_TEXT_2";
    public static final String PAR_CARD_TRANSLATE_EXAMPLE_2 = "NewCard_PAR_CARD_TRANSLATE_EXAMPLE_2";
    public static final String PAR_CARD_TRANSLATE_NOTES = "NewCard_PAR_CARD_TRANSLATE_NOTES";

    public static final String ACT_CREATE_CARD = "NewCard_ACT_CREATE_CARD";
    public static final String ACT_CARD_TRANSLATE_RESTORE_PREV_TEXT1 = "NewCard_ACT_CARD_TRANSLATE_RESTORE_PREV_TEXT1";
    public static final String ACT_CARD_TRANSLATE_RESTORE_PREV_EXAMPLE1 = "NewCard_ACT_CARD_TRANSLATE_RESTORE_PREV_EXAMPLE1";
    public static final String ACT_CARD_TRANSLATE_RESTORE_PREV_TEXT2 = "NewCard_ACT_CARD_TRANSLATE_RESTORE_PREV_TEXT2";
    public static final String ACT_CARD_TRANSLATE_RESTORE_PREV_EXAMPLE2 = "NewCard_ACT_CARD_TRANSLATE_RESTORE_PREV_EXAMPLE2";

    private final Settings settings;
    private final CardExplorerConstructor cardExplorerConstructor;

    @Override
    public HtmlElem render(NewCardState st) {
        return simplePageWithTitle("Add new card",
            rndErrors(st.getErrors()),
            h3(text("Add new card")),
            form(
                div(
                    rndDirSelector(st.getDir()),
                    rndCardType(st.getCard().getType())
                ),
                br(),
                div(rndCard(st.getCard())),
                br(),
                inpSubmit(ACT_CREATE_CARD, "Save").attr("tabindex", "3")
            )
        );
    }

    private HtmlElem rndCard(CardDto cardDto) {
        return switch (cardDto) {
            case CardDto.FillGaps dto -> rndCardFillGaps(dto);
            case CardDto.Translate dto -> rndCardTranslate(dto);
        };
    }

    private HtmlElem rndCardFillGaps(CardDto.FillGaps card) {
        return table(List.of(
            List.of(
                text("Language"),
                rndAvailableLanguages(settings, card.getLang(), PAR_CARD_FILL_GAPS_LANG)
            ),
            List.of(
                text("Text"),
                table(List.of(
                    List.of(div("color:grey;",
                        text("[[word|translation|transcription]] or [[answer|hint|notes]]"),
                        inpButton("", "Mark selected")
                            .attr("onclick", "markSelectedText('%s')".formatted(PAR_CARD_FILL_GAPS_TEXT))
                    )),
                    List.of(
                        textarea(PAR_CARD_FILL_GAPS_TEXT, card.getText(), 100, 5).autofocus()
                            .attr("tabindex", "2")
                            .id(PAR_CARD_FILL_GAPS_TEXT)
                    )
                ))
            ),
            List.of(
                text("Notes"),
                textarea(PAR_CARD_FILL_GAPS_NOTES, card.getNotes(), 100, 5)
            )
        ));
    }

    private HtmlElem rndCardTranslate(CardDto.Translate card) {
        return table(List.of(
            List.of(
                text("Language 1"),
                table(List.of(List.of(
                    rndAvailableLanguages(settings, card.getLang1(), PAR_CARD_TRANSLATE_LANG_1),
                    select(PAR_CARD_TRANSLATE_EXACT_MATCH_1, String.valueOf(card.isExactMatch1()), List.of(
                        Pair.of("true", text("= (exact match)")),
                        Pair.of("false", text("~ (approximate match)"))
                    )).id(PAR_CARD_TRANSLATE_EXACT_MATCH_1)
                )))
            ),
            List.of(
                rndClickableText("Text 1", ACT_CARD_TRANSLATE_RESTORE_PREV_TEXT1),
                textarea(PAR_CARD_TRANSLATE_TEXT_1, card.getText1(), 100, 5).attr("autofocus", "")
                    .attr("tabindex", "1")
                    .onkeydown(format("toggleExactMatch(event,'%s')", PAR_CARD_TRANSLATE_EXACT_MATCH_1))
            ),
            List.of(
                rndClickableText("Example 1", ACT_CARD_TRANSLATE_RESTORE_PREV_EXAMPLE1),
                textarea(PAR_CARD_TRANSLATE_EXAMPLE_1, card.getExample1(), 100, 1)
            ),
            List.of(
                div("height:30px"),
                div("height:30px")
            ),
            List.of(
                text("Language 2"),
                table(List.of(List.of(
                    rndAvailableLanguages(settings, card.getLang2(), PAR_CARD_TRANSLATE_LANG_2),
                    select(PAR_CARD_TRANSLATE_EXACT_MATCH_2, String.valueOf(card.isExactMatch2()), List.of(
                        Pair.of("true", text("= (exact match)")),
                        Pair.of("false", text("~ (approximate match)"))
                    )).id(PAR_CARD_TRANSLATE_EXACT_MATCH_2)
                )))
            ),
            List.of(
                rndClickableText("Text 2", ACT_CARD_TRANSLATE_RESTORE_PREV_TEXT2),
                textarea(PAR_CARD_TRANSLATE_TEXT_2, card.getText2(), 100, 5)
                    .attr("tabindex", "2")
                    .onkeydown(format("toggleExactMatch(event,'%s')", PAR_CARD_TRANSLATE_EXACT_MATCH_2))
            ),
            List.of(
                rndClickableText("Example 2", ACT_CARD_TRANSLATE_RESTORE_PREV_EXAMPLE2),
                textarea(PAR_CARD_TRANSLATE_EXAMPLE_2, card.getExample2(), 100, 1)
            ),
            List.of(
                div("height:30px"),
                div("height:30px")
            ),
            List.of(
                text("Notes"),
                textarea(PAR_CARD_TRANSLATE_NOTES, card.getNotes(), 100, 5)
            )
        ));
    }

    private HtmlElem rndAvailableLanguages(Settings settings, String selectedLang, String paramName) {
        return select(paramName, selectedLang,
            settings.getLanguages().stream()
                .map(lang -> Pair.of(lang, text(lang)))
                .toList()
        );
    }

    private HtmlElem rndCardType(CardType cardType) {
        return table(List.of(List.of(
            text("Card type"),
            select(
                PAR_CARD_TYPE,
                cardType.getCode(),
                Arrays.stream(CardType.values())
                    .map(typ -> Pair.of(typ.getCode(), text(typ.getDisplayName())))
                    .toList()
            ).submitOnChange()
        )));
    }

    private HtmlTag rndDirSelector(DirSelectorCmp dir) {
        return table(List.of(List.of(
            a(
                cardExplorerConstructor.makeUrlWithInitPath(dir.getSelectedDirectoryStr()),
                text("Directory")
            ),
            frag(dir.render())
        )));
    }

    private HtmlElem rndClickableText(String text, String btnId) {
        return frag(
            span("cursor:pointer", text(text)).attr("onclick", "clickBtn('%s')".formatted(btnId)),
            inpSubmit(btnId, "").attr("style", "display:none")
        );
    }
}
