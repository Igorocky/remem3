package org.igye.remem3.controllers.newcard;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.igye.remem3.app.App;
import org.igye.remem3.app.Cache;
import org.igye.remem3.app.CardUtils;
import org.igye.remem3.app.Settings;
import org.igye.remem3.app.dto.Card;
import org.igye.remem3.app.dto.CardType;
import org.igye.remem3.app.impl.CacheImpl;
import org.igye.remem3.app.impl.CardUtilsImpl;
import org.igye.remem3.app.impl.SettingsImpl;
import org.igye.remem3.controllers.components.DirSelectorCmp;
import org.igye.remem3.controllers.components.impl.DirSelectorCmpImpl;
import org.igye.remem3.html.HtmlBuilder;
import org.igye.remem3.html.HtmlElem;
import org.igye.remem3.html.HtmlTag;
import org.igye.remem3.utils.Utils;
import org.igye.remem3.web.RequestParams;
import org.igye.remem3.web.StatefulWebController;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.igye.remem3.app.impl.CardUtilsImpl.CARD_FILL_GAPS_FILE_EXTENSION;
import static org.igye.remem3.app.impl.CardUtilsImpl.CARD_TRANSLATE_FILE_EXTENSION;

public class NewCardController extends HtmlBuilder
    implements StatefulWebController<NewCardState, Supplier<NewCardState>> {

    private static final String PAR_DIR_TO_SAVE_NEW_CARD_TO = "PAR_DIR_TO_SAVE_NEW_CARD_TO";
    private static final String PAR_CARD_TYPE = "PAR_CARD_TYPE";

    private static final String PAR_CARD_FILL_GAPS_LANG = "PAR_CARD_FILL_GAPS_LANG";
    private static final String PAR_CARD_FILL_GAPS_TEXT = "PAR_CARD_FILL_GAPS_TEXT";
    private static final String PAR_CARD_FILL_GAPS_NOTES = "PAR_CARD_FILL_GAPS_NOTES";

    private static final String PAR_CARD_TRANSLATE_LANG_1 = "PAR_CARD_TRANSLATE_LANG_1";
    private static final String PAR_CARD_TRANSLATE_EXACT_MATCH_1 = "PAR_CARD_TRANSLATE_EXACT_MATCH_1";
    private static final String PAR_CARD_TRANSLATE_TEXT_1 = "PAR_CARD_TRANSLATE_TEXT_1";
    private static final String PAR_CARD_TRANSLATE_LANG_2 = "PAR_CARD_TRANSLATE_LANG_2";
    private static final String PAR_CARD_TRANSLATE_EXACT_MATCH_2 = "PAR_CARD_TRANSLATE_EXACT_MATCH_2";
    private static final String PAR_CARD_TRANSLATE_TEXT_2 = "PAR_CARD_TRANSLATE_TEXT_2";
    private static final String PAR_CARD_TRANSLATE_NOTES = "PAR_CARD_TRANSLATE_NOTES";

    private static final String ACT_CREATE_CARD = "ACT_CREATE_CARD";

    private final App app;
    private final Utils utils;

    public NewCardController(App app) {
        this.app = app;
        this.utils = app.getUtils();
    }

    @Override
    public String getPath() {
        return "create_new_card";
    }

    @Override
    public NewCardState loadState(RequestParams params) {
        try {
            app.reloadProperties();
            Settings settings = SettingsImpl.load(app);
            Cache cache = CacheImpl.load(utils, settings);
            DirSelectorCmp dirSelector = new DirSelectorCmpImpl(settings, cache, params, PAR_DIR_TO_SAVE_NEW_CARD_TO);
            return NewCardState.builder()
                .settings(settings)
                .cache(cache)
                .dirSelector(dirSelector)
                .cardParams(makeCardParams(params, settings, cache))
                .build();
        } catch (Exception ex) {
            return NewCardState.builder()
                .errors(List.of(ex.getMessage()))
                .build();
        }
    }

    @Override
    public Optional<Supplier<NewCardState>> decodeAction(RequestParams params, NewCardState state) {
        if (CollectionUtils.isNotEmpty(state.getErrors())) {
            return Optional.empty();
        }
        if (params.hasParam(ACT_CREATE_CARD)) {
            return Optional.of(() -> actCreateCard(state));
        }
        return Optional.empty();
    }

    @Override
    public NewCardState updateState(NewCardState state, Supplier<NewCardState> action) {
        if (CollectionUtils.isNotEmpty(state.getErrors())) {
            return state;
        }
        return action.get();
    }

    @Override
    public void saveState(NewCardState state) {
        state.getCache().put(PAR_CARD_TYPE, state.getCardParams().getType().getCode());
    }

    @Override
    public String renderState(NewCardState st) {
        return simplePageWithTitle("Add new card",
            rndErrors(st.getErrors()),
            h3(text("Add new card")),
            form(
                div(
                    rndDirSelector(st),
                    rndCardType(st)
                ),
                br(),
                div(rndCard(st)),
                br(),
                inpSubmit(ACT_CREATE_CARD, "Save")
            )
        ).toString();
    }

    private HtmlTag rndDirSelector(NewCardState st) {
        return table(List.of(List.of(
            text("Directory"),
            frag(st.getDirSelector().render())
        )));
    }

    private NewCardState actCreateCard(NewCardState st) {
        try {
            String dirStr = st.getDirSelector().getSelectedDirectoryStr();
            File dir = new File(dirStr);
            if (!dir.exists()) {
                dir.mkdirs();
                if (!dir.exists()) {
                    return st.withErrors(List.of(String.format("Cannot create a directory: %s", dirStr)));
                }
            } else if (!dir.isDirectory()) {
                return st.withErrors(List.of(String.format("Not a directory: %s", dirStr)));
            }
            CardUtils cardUtils = new CardUtilsImpl(utils, st.getSettings());
            CardDto cardDto = st.getCardParams();
            Card card = cardUtils.makeCard(cardDto);
            List<String> errors = cardUtils.validateCard(card);
            if (CollectionUtils.isNotEmpty(errors)) {
                return st.withErrors(errors);
            }
            st.getCache().put(PAR_DIR_TO_SAVE_NEW_CARD_TO, dirStr);
            int _ = switch (cardDto) {
                case CardDto.FillGaps dto -> {
                    st.getCache().put(PAR_CARD_FILL_GAPS_LANG, dto.getLang());
                    yield 1;
                }
                case CardDto.Translate dto -> {
                    st.getCache().put(PAR_CARD_TRANSLATE_LANG_1, dto.getLang1());
                    st.getCache().put(PAR_CARD_TRANSLATE_LANG_2, dto.getLang2());
                    yield 1;
                }
            };
            cardUtils.saveCard(new File(dir, makeFileName(card)), card);
            return st.withCardParams(clearParams(cardDto));
        } catch (Exception ex) {
            return st.withErrors(List.of(ex.getMessage()));
        }
    }

    private CardDto clearParams(CardDto dto) {
        return switch (dto) {
            case CardDto.FillGaps c -> c.withText("");
            case CardDto.Translate c -> c.withText1("").withText2("");
        };
    }

    private String makeFileName(Card card) {
        String baseName = UUID.randomUUID().toString().replace("-", "_");
        String extension = switch (card) {
            case Card.FillGaps _ -> CARD_FILL_GAPS_FILE_EXTENSION;
            case Card.Translate _ -> CARD_TRANSLATE_FILE_EXTENSION;
        };
        return baseName + extension;
    }

    private HtmlElem rndCard(NewCardState st) {
        CardDto cardParams = st.getCardParams();
        return switch (cardParams) {
            case CardDto.FillGaps dto -> rndCardFillGaps(st, dto);
            case CardDto.Translate dto -> rndCardTranslate(st, dto);
        };
    }

    private HtmlElem rndCardFillGaps(NewCardState st, CardDto.FillGaps card) {
        return table(List.of(
            List.of(
                text("Language"),
                rndAvailableLanguages(st.getSettings(), card.getLang(), PAR_CARD_FILL_GAPS_LANG)
            ),
            List.of(
                text("Text"),
                table(List.of(
                    List.of(div("color:grey;", text("[[word|translation|transcription]] or [[answer|hint|notes]]"))),
                    List.of(textarea(PAR_CARD_FILL_GAPS_TEXT, card.getText(), 100, 5))
                ))
            ),
            List.of(
                text("Notes"),
                textarea(PAR_CARD_FILL_GAPS_NOTES, card.getNotes(), 100, 5)
            )
        ));
    }

    private HtmlElem rndCardTranslate(NewCardState st, CardDto.Translate card) {
        return table(List.of(
            List.of(
                text("Language 1"),
                table(List.of(List.of(
                    rndAvailableLanguages(st.getSettings(), card.getLang1(), PAR_CARD_TRANSLATE_LANG_1),
                    select(PAR_CARD_TRANSLATE_EXACT_MATCH_1, false, String.valueOf(card.isExactMatch1()), List.of(
                        Pair.of("true", text("Exact match")),
                        Pair.of("false", text("Approximate match"))
                    ))
                )))
            ),
            List.of(
                text("Text 1"),
                textarea(PAR_CARD_TRANSLATE_TEXT_1, card.getText1(), 100, 5)
            ),
            List.of(
                div("height:30px"),
                div("height:30px")
            ),
            List.of(
                text("Language 2"),
                table(List.of(List.of(
                    rndAvailableLanguages(st.getSettings(), card.getLang2(), PAR_CARD_TRANSLATE_LANG_2),
                    select(PAR_CARD_TRANSLATE_EXACT_MATCH_2, false, String.valueOf(card.isExactMatch2()), List.of(
                        Pair.of("true", text("Exact match")),
                        Pair.of("false", text("Approximate match"))
                    ))
                )))
            ),
            List.of(
                text("Text 2"),
                textarea(PAR_CARD_TRANSLATE_TEXT_2, card.getText2(), 100, 5)
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

    private HtmlElem rndCardType(NewCardState state) {
        return table(List.of(List.of(
            text("Card type"),
            select(
                PAR_CARD_TYPE,
                true,
                state.getCardParams().getType().getCode(),
                Arrays.stream(CardType.values())
                    .map(cardType -> Pair.of(cardType.getCode(), text(cardType.getDisplayName())))
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

    private CardDto makeCardParams(RequestParams params, Settings settings, Cache cache) {
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
            case FILL_GAPS -> makeFillGapsCardParams(params, settings, cache);
            case TRANSLATE -> makeTranslateCardParams(params, settings, cache);
        };
    }

    private CardDto.FillGaps makeFillGapsCardParams(RequestParams params, Settings settings, Cache cache) {
        return CardDto.FillGaps.builder()
            .lang(params.getParam(
                PAR_CARD_FILL_GAPS_LANG,
                cache.getStr(PAR_CARD_FILL_GAPS_LANG, settings.getLanguages().getFirst())
            ))
            .text(params.getParam(PAR_CARD_FILL_GAPS_TEXT, ""))
            .notes(params.getParam(PAR_CARD_FILL_GAPS_NOTES, ""))
            .build();
    }

    private CardDto.Translate makeTranslateCardParams(RequestParams params, Settings settings, Cache cache) {
        return CardDto.Translate.builder()
            .lang1(params.getParam(
                PAR_CARD_TRANSLATE_LANG_1,
                cache.getStr(PAR_CARD_TRANSLATE_LANG_1, settings.getLanguages().getFirst())
            ))
            .text1(params.getParam(PAR_CARD_TRANSLATE_TEXT_1, ""))
            .exactMatch1(isExactMatch(PAR_CARD_TRANSLATE_EXACT_MATCH_1, params))
            .lang2(params.getParam(
                PAR_CARD_TRANSLATE_LANG_2,
                cache.getStr(PAR_CARD_TRANSLATE_LANG_2, settings.getLanguages().getFirst())
            ))
            .text2(params.getParam(PAR_CARD_TRANSLATE_TEXT_2, ""))
            .exactMatch2(isExactMatch(PAR_CARD_TRANSLATE_EXACT_MATCH_2, params))
            .notes(params.getParam(PAR_CARD_TRANSLATE_NOTES, ""))
            .build();
    }

    private boolean isExactMatch(String paramName, RequestParams params) {
        return !params.hasParam(paramName) || Boolean.parseBoolean(params.getParam(paramName));
    }
}
