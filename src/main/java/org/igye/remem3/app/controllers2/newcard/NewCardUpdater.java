package org.igye.remem3.app.controllers2.newcard;

import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.igye.remem3.app.Cache;
import org.igye.remem3.app.CardUtils;
import org.igye.remem3.app.dto.Card;
import org.igye.remem3.app.state.StateUpdater;
import org.igye.remem3.html.HtmlBuilder;
import org.igye.remem3.web.RequestParams;

import java.io.File;
import java.util.List;

import static org.igye.remem3.app.controllers2.newcard.NewCardRenderer.ACT_CREATE_CARD;
import static org.igye.remem3.app.controllers2.newcard.NewCardRenderer.PAR_CARD_FILL_GAPS_LANG;
import static org.igye.remem3.app.controllers2.newcard.NewCardRenderer.PAR_CARD_TRANSLATE_EXACT_MATCH_1;
import static org.igye.remem3.app.controllers2.newcard.NewCardRenderer.PAR_CARD_TRANSLATE_EXACT_MATCH_2;
import static org.igye.remem3.app.controllers2.newcard.NewCardRenderer.PAR_CARD_TRANSLATE_LANG_1;
import static org.igye.remem3.app.controllers2.newcard.NewCardRenderer.PAR_CARD_TRANSLATE_LANG_2;
import static org.igye.remem3.app.controllers2.newcard.NewCardRenderer.PAR_CARD_TYPE;
import static org.igye.remem3.app.controllers2.newcard.NewCardRenderer.PAR_DIR_TO_SAVE_NEW_CARD_TO;

@RequiredArgsConstructor
public class NewCardUpdater extends HtmlBuilder implements StateUpdater<NewCardState> {

    private final Cache cache;
    private final NewCardUtils newCardUtils;
    private final CardUtils cardUtils;


    @Override
    public NewCardState update(NewCardState state, RequestParams params) {
        NewCardState updatedState = newCardUtils.readStateFromParams(params);
        if (params.hasParam(ACT_CREATE_CARD)) {
            updatedState = saveCard(updatedState);
            if (CollectionUtils.isEmpty(updatedState.getErrors())) {
                cacheSomeSavedParams(updatedState);
                updatedState = clearParamsAfterSave(updatedState);
            }
        }
        return updatedState;
    }

    private NewCardState saveCard(NewCardState st) {
        try {
            String dirStr = st.getDir().getSelectedDirectoryStr();
            File dir = new File(dirStr);
            if (!dir.exists()) {
                dir.mkdirs();
                if (!dir.exists()) {
                    return st.withErrors(List.of("Cannot create a directory: %s".formatted(dirStr)));
                }
            } else if (!dir.isDirectory()) {
                return st.withErrors(List.of("Not a directory: %s".formatted(dirStr)));
            }
            CardDto cardDto = st.getCard();
            Card card = cardUtils.makeCard(cardDto);
            List<String> errors = cardUtils.validateCard(card);
            if (CollectionUtils.isNotEmpty(errors)) {
                return st.withErrors(errors);
            }
            cardUtils.saveCard(new File(dir, cardUtils.makeFileNameForCard(card)), card);
            return st;
        } catch (Exception ex) {
            return st.withErrors(List.of(ex.getMessage()));
        }
    }

    private void cacheSomeSavedParams(NewCardState st) {
        cache.put(PAR_CARD_TYPE, st.getCard().getType().getCode());
        cache.put(PAR_DIR_TO_SAVE_NEW_CARD_TO, st.getDir().getSelectedDirectoryStr());
        int _ = switch (st.getCard()) {
            case CardDto.FillGaps dto -> {
                cache.put(PAR_CARD_FILL_GAPS_LANG, dto.getLang());
                yield 1;
            }
            case CardDto.Translate dto -> {
                cache.put(PAR_CARD_TRANSLATE_LANG_1, dto.getLang1());
                cache.put(PAR_CARD_TRANSLATE_EXACT_MATCH_1, dto.isExactMatch1());
                cache.put(PAR_CARD_TRANSLATE_LANG_2, dto.getLang2());
                cache.put(PAR_CARD_TRANSLATE_EXACT_MATCH_2, dto.isExactMatch2());
                yield 1;
            }
        };
    }

    private NewCardState clearParamsAfterSave(NewCardState st) {
        return st.withCard(
            switch (st.getCard()) {
                case CardDto.FillGaps c -> c.withText("");
                case CardDto.Translate c -> c.withText1("").withExample1("").withText2("").withExample2("");
            }
        );
    }
}
