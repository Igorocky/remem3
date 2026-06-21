package org.igye.remem3.app.controllers.newcard;

import lombok.RequiredArgsConstructor;
import org.igye.remem3.app.Cache;
import org.igye.remem3.app.Settings;
import org.igye.remem3.app.components.impl.DirSelectorCmpImpl;
import org.igye.remem3.app.dto.CardType;
import org.igye.remem3.web.RequestParams;

import java.util.List;

import static org.igye.remem3.app.controllers.newcard.NewCardRenderer.PAR_CARD_FILL_GAPS_LANG;
import static org.igye.remem3.app.controllers.newcard.NewCardRenderer.PAR_CARD_FILL_GAPS_NOTES;
import static org.igye.remem3.app.controllers.newcard.NewCardRenderer.PAR_CARD_FILL_GAPS_TEXT;
import static org.igye.remem3.app.controllers.newcard.NewCardRenderer.PAR_CARD_TRANSLATE_EXACT_MATCH_1;
import static org.igye.remem3.app.controllers.newcard.NewCardRenderer.PAR_CARD_TRANSLATE_EXACT_MATCH_2;
import static org.igye.remem3.app.controllers.newcard.NewCardRenderer.PAR_CARD_TRANSLATE_EXAMPLE_1;
import static org.igye.remem3.app.controllers.newcard.NewCardRenderer.PAR_CARD_TRANSLATE_EXAMPLE_2;
import static org.igye.remem3.app.controllers.newcard.NewCardRenderer.PAR_CARD_TRANSLATE_LANG_1;
import static org.igye.remem3.app.controllers.newcard.NewCardRenderer.PAR_CARD_TRANSLATE_LANG_2;
import static org.igye.remem3.app.controllers.newcard.NewCardRenderer.PAR_CARD_TRANSLATE_NOTES;
import static org.igye.remem3.app.controllers.newcard.NewCardRenderer.PAR_CARD_TRANSLATE_TEXT_1;
import static org.igye.remem3.app.controllers.newcard.NewCardRenderer.PAR_CARD_TRANSLATE_TEXT_2;
import static org.igye.remem3.app.controllers.newcard.NewCardRenderer.PAR_CARD_TYPE;
import static org.igye.remem3.app.controllers.newcard.NewCardRenderer.PAR_DIR_TO_SAVE_NEW_CARD_TO;

@RequiredArgsConstructor
public class NewCardUtilsImpl implements NewCardUtils {
    private final Settings settings;
    private final Cache cache;

    @Override
    public NewCardState readStateFromParams(RequestParams params) {
        return NewCardState.builder()
            .errors(List.of())
            .dir(
                new DirSelectorCmpImpl(settings, cache, PAR_DIR_TO_SAVE_NEW_CARD_TO).setPath(params).setAllowMkDir(true)
            )
            .card(readCardFromParams(params))
            .build();
    }

    private CardDto readCardFromParams(RequestParams params) {
        CardType defaultCardType = CardType.FILL_GAPS;
        CardType cardType;
        try {
            if (params.hasParam(PAR_CARD_TYPE)) {
                cardType = CardType.fromCode(params.getParam(PAR_CARD_TYPE));
            } else {
                cardType = CardType.fromCode(cache.getStr(PAR_CARD_TYPE, defaultCardType.getCode()));
            }
        } catch (Exception e) {
            cardType = defaultCardType;
        }
        return switch (cardType) {
            case FILL_GAPS -> readFillGapsCardFromParams(params);
            case TRANSLATE -> readTranslateCardFromParams(params);
        };
    }

    private CardDto readFillGapsCardFromParams(RequestParams params) {
        return CardDto.FillGaps.builder()
            .lang(params.getParam(
                PAR_CARD_FILL_GAPS_LANG,
                cache.getStr(PAR_CARD_FILL_GAPS_LANG, settings.getLanguages().getFirst())
            ))
            .text(params.getParam(PAR_CARD_FILL_GAPS_TEXT, ""))
            .notes(params.getParam(PAR_CARD_FILL_GAPS_NOTES, ""))
            .build();
    }

    private CardDto readTranslateCardFromParams(RequestParams params) {
        return CardDto.Translate.builder()
            .lang1(params.getParam(
                PAR_CARD_TRANSLATE_LANG_1,
                cache.getStr(PAR_CARD_TRANSLATE_LANG_1, settings.getLanguages().getFirst())
            ))
            .text1(params.getParam(PAR_CARD_TRANSLATE_TEXT_1, ""))
            .exactMatch1(isExactMatch(PAR_CARD_TRANSLATE_EXACT_MATCH_1, params))
            .example1(params.getParam(PAR_CARD_TRANSLATE_EXAMPLE_1, ""))
            .lang2(params.getParam(
                PAR_CARD_TRANSLATE_LANG_2,
                cache.getStr(PAR_CARD_TRANSLATE_LANG_2, settings.getLanguages().getFirst())
            ))
            .text2(params.getParam(PAR_CARD_TRANSLATE_TEXT_2, ""))
            .exactMatch2(isExactMatch(PAR_CARD_TRANSLATE_EXACT_MATCH_2, params))
            .example2(params.getParam(PAR_CARD_TRANSLATE_EXAMPLE_2, ""))
            .notes(params.getParam(PAR_CARD_TRANSLATE_NOTES, ""))
            .build();
    }

    private boolean isExactMatch(String paramName, RequestParams params) {
        if (params.hasParam(paramName)) {
            return Boolean.parseBoolean(params.getParam(paramName));
        } else {
            return cache.getBool(paramName, true);
        }
    }
}
