package org.igye.remem3.app.controllers2.newcard;

import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.igye.remem3.app.Cache;
import org.igye.remem3.app.Settings;
import org.igye.remem3.app.controllers.components.DirSelectorCmp;
import org.igye.remem3.app.state.StateRenderer;
import org.igye.remem3.html.HtmlBuilder;
import org.igye.remem3.html.HtmlElem;
import org.igye.remem3.html.HtmlTag;

import java.util.List;

@RequiredArgsConstructor
public class MakeNewCardRenderer extends HtmlBuilder implements StateRenderer<MakeNewCardState> {
    private static final String PAR_DIR_TO_SAVE_NEW_CARD_TO = "MakeNewCard__PAR_DIR_TO_SAVE_NEW_CARD_TO";
    private static final String PAR_CARD_TYPE = "MakeNewCard__PAR_CARD_TYPE";

    private static final String PAR_CARD_FILL_GAPS_LANG = "MakeNewCard__PAR_CARD_FILL_GAPS_LANG";
    private static final String PAR_CARD_FILL_GAPS_TEXT = "MakeNewCard__PAR_CARD_FILL_GAPS_TEXT";
    private static final String PAR_CARD_FILL_GAPS_NOTES = "MakeNewCard__PAR_CARD_FILL_GAPS_NOTES";

    private static final String PAR_CARD_TRANSLATE_LANG_1 = "MakeNewCard__PAR_CARD_TRANSLATE_LANG_1";
    private static final String PAR_CARD_TRANSLATE_EXACT_MATCH_1 = "MakeNewCard__PAR_CARD_TRANSLATE_EXACT_MATCH_1";
    private static final String ID_PAR_CARD_TRANSLATE_EXACT_MATCH_1 = "ID_" + PAR_CARD_TRANSLATE_EXACT_MATCH_1;
    private static final String PAR_CARD_TRANSLATE_TEXT_1 = "MakeNewCard__PAR_CARD_TRANSLATE_TEXT_1";
    private static final String PAR_CARD_TRANSLATE_EXAMPLE_1 = "MakeNewCard__PAR_CARD_TRANSLATE_EXAMPLE_1";
    private static final String PAR_CARD_TRANSLATE_LANG_2 = "MakeNewCard__PAR_CARD_TRANSLATE_LANG_2";
    private static final String PAR_CARD_TRANSLATE_EXACT_MATCH_2 = "MakeNewCard__PAR_CARD_TRANSLATE_EXACT_MATCH_2";
    private static final String ID_PAR_CARD_TRANSLATE_EXACT_MATCH_2 = "ID_" + PAR_CARD_TRANSLATE_EXACT_MATCH_2;
    private static final String PAR_CARD_TRANSLATE_TEXT_2 = "MakeNewCard__PAR_CARD_TRANSLATE_TEXT_2";
    private static final String PAR_CARD_TRANSLATE_EXAMPLE_2 = "MakeNewCard__PAR_CARD_TRANSLATE_EXAMPLE_2";
    private static final String PAR_CARD_TRANSLATE_NOTES = "MakeNewCard__PAR_CARD_TRANSLATE_NOTES";

    private static final String ACT_CREATE_CARD = "MakeNewCard__ACT_CREATE_CARD";

    private final Settings settings;
    private final Cache cache;

    @Override
    public Class<?> getSupportedType() {
        return MakeNewCardState.class;
    }

    @Override
    public String render(MakeNewCardState st) {
        return simplePageWithTitle("Add new card",
            rndErrors(st.getErrors()),
            h3(text("Add new card")),
            form(
                div(
                    rndDirSelector(st.getDir())
//                    rndCardType(st)
                ),
                br(),
//                div(rndCard(st)),
                br(),
                inpSubmit(ACT_CREATE_CARD, "Save").attr("tabindex", "3")
            )
        ).toString();
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
