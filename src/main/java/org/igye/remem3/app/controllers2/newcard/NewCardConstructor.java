package org.igye.remem3.app.controllers2.newcard;

import lombok.RequiredArgsConstructor;
import org.igye.remem3.app.Cache;
import org.igye.remem3.app.Settings;
import org.igye.remem3.app.controllers.components.impl.DirSelectorCmpImpl;
import org.igye.remem3.app.controllers.newcard.CardDto;
import org.igye.remem3.app.dto.CardType;
import org.igye.remem3.app.state.StateConstructor;

import java.io.File;
import java.util.List;

import static org.igye.remem3.app.controllers2.newcard.NewCardRenderer.PAR_CARD_FILL_GAPS_LANG;
import static org.igye.remem3.app.controllers2.newcard.NewCardRenderer.PAR_CARD_TRANSLATE_EXACT_MATCH_1;
import static org.igye.remem3.app.controllers2.newcard.NewCardRenderer.PAR_CARD_TRANSLATE_EXACT_MATCH_2;
import static org.igye.remem3.app.controllers2.newcard.NewCardRenderer.PAR_CARD_TRANSLATE_LANG_1;
import static org.igye.remem3.app.controllers2.newcard.NewCardRenderer.PAR_CARD_TRANSLATE_LANG_2;
import static org.igye.remem3.app.controllers2.newcard.NewCardRenderer.PAR_CARD_TYPE;
import static org.igye.remem3.app.controllers2.newcard.NewCardRenderer.PAR_DIR_TO_SAVE_NEW_CARD_TO;

@RequiredArgsConstructor
public class NewCardConstructor implements StateConstructor<NewCardState> {
    private final Settings settings;
    private final Cache cache;

    @Override
    public String getName() {
        return "new_card";
    }

    @Override
    public NewCardState construct() {
        File dir = new File(cache.getStr(PAR_DIR_TO_SAVE_NEW_CARD_TO, ""));
        DirSelectorCmpImpl dirCmp = new DirSelectorCmpImpl(settings, cache, PAR_DIR_TO_SAVE_NEW_CARD_TO, false, dir);
        return NewCardState.builder()
            .errors(List.of())
            .dir(dirCmp)
            .card(makeDefaultCard())
            .build();
    }

    @Override
    public boolean isSingleton() {
        return false;
    }

    @Override
    public Class<?> getSupportedType() {
        return NewCardState.class;
    }

    private CardDto makeDefaultCard() {
        CardType defaultCardType = CardType.FILL_GAPS;
        CardType cardType = CardType.fromCode(cache.getStr(PAR_CARD_TYPE, defaultCardType.getCode()));
        return switch (cardType) {
            case FILL_GAPS -> makeDefaultFillGapsCard();
            case TRANSLATE -> makeDefaultTranslateCard();
        };
    }

    private CardDto.FillGaps makeDefaultFillGapsCard() {
        return CardDto.FillGaps.builder()
            .lang(cache.getStr(PAR_CARD_FILL_GAPS_LANG, settings.getLanguages().getFirst()))
            .text("")
            .notes("")
            .build();
    }

    private CardDto.Translate makeDefaultTranslateCard() {
        return CardDto.Translate.builder()
            .lang1(cache.getStr(PAR_CARD_TRANSLATE_LANG_1, settings.getLanguages().getFirst()))
            .text1("")
            .exactMatch1(cache.getBool(PAR_CARD_TRANSLATE_EXACT_MATCH_1, true))
            .example1("")
            .lang2(cache.getStr(PAR_CARD_TRANSLATE_LANG_2, settings.getLanguages().getFirst()))
            .text2("")
            .exactMatch2(cache.getBool(PAR_CARD_TRANSLATE_EXACT_MATCH_2, true))
            .example2("")
            .notes("")
            .build();
    }
}
