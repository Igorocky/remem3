package org.igye.remem3.controllers.newcard;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.igye.remem3.app.App;
import org.igye.remem3.app.Cache;
import org.igye.remem3.app.Cards;
import org.igye.remem3.app.Settings;
import org.igye.remem3.app.dto.Card;
import org.igye.remem3.app.dto.CardType;
import org.igye.remem3.app.impl.CacheImpl;
import org.igye.remem3.app.impl.CardsImpl;
import org.igye.remem3.app.impl.SettingsImpl;
import org.igye.remem3.html.HtmlBuilder;
import org.igye.remem3.html.HtmlElem;
import org.igye.remem3.utils.Utils;
import org.igye.remem3.utils.web.RequestParams;
import org.igye.remem3.utils.web.impl.RequestParamsImpl;
import org.igye.remem3.web.StatefulWebController;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.igye.remem3.app.impl.CardsImpl.CARD_FILL_GAPS_FILE_EXTENSION;

@RequiredArgsConstructor
public class NewCardController extends HtmlBuilder
    implements StatefulWebController<NewCardState, Supplier<NewCardState>> {

    private static final String PAR_DIR_TO_SAVE_NEW_CARD_TO = "PAR_DIR_TO_SAVE_NEW_CARD_TO";
    private static final String PAR_CARD_TYPE = "PAR_CARD_TYPE";
    private static final String PAR_CARD_FILL_GAPS_LANG = "PAR_CARD_FILL_GAPS_LANG";
    private static final String PAR_CARD_FILL_GAPS_TEXT = "PAR_CARD_FILL_GAPS_TEXT";
    private static final String PAR_CARD_FILL_GAPS_NOTES = "PAR_CARD_FILL_GAPS_NOTES";
    private static final String ACT_CREATE_CARD = "ACT_CREATE_CARD";

    private final App app;
    private final Utils utils;

    @Override
    public String getPath() {
        return "create_new_card";
    }

    @Override
    public NewCardState loadState(HttpServletRequest req) {
        try {
            app.reloadProperties();
            Settings settings = SettingsImpl.load(app);
            Cache cache = CacheImpl.load(utils, settings);
            RequestParams params = new RequestParamsImpl(req);
            return NewCardState.builder()
                .settings(settings)
                .cache(cache)
                .dir(getDir(params, settings, cache))
                .cardParams(makeCardParams(params, settings, cache))
                .build();
        } catch (Exception ex) {
            return NewCardState.builder()
                .errors(List.of(ex.getMessage()))
                .build();
        }
    }

    @Override
    public Optional<Supplier<NewCardState>> decodeAction(HttpServletRequest req, NewCardState state) {
        if (CollectionUtils.isNotEmpty(state.getErrors())) {
            return Optional.empty();
        }
        RequestParams params = new RequestParamsImpl(req);
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

    }

    @Override
    public String renderState(NewCardState st) {
        return simplePageWithTitle("Add new card",
            rndErrors(st.getErrors()),
            h3(text("Add new card")),
            form(
                rndDirSelector(st),
                rndCardType(st),
                rndCard(st),
                inpSubmit(ACT_CREATE_CARD, "Save")
            )
        ).toString();
    }

    private NewCardState actCreateCard(NewCardState st) {
        try {
            String dirStr = st.getDir();
            File dir = new File(dirStr);
            if (!dir.exists()) {
                dir.mkdirs();
                if (!dir.exists()) {
                    return st.withErrors(List.of(String.format("Cannot create a directory: %s", dirStr)));
                }
            } else if (!dir.isDirectory()) {
                return st.withErrors(List.of(String.format("Not a directory: %s", dirStr)));
            }
            Cards cards = new CardsImpl(utils, st.getSettings());
            CardDto cardDto = st.getCardParams();
            Card card = cards.makeCard(cardDto);
            List<String> errors = cards.validateCard(card);
            if (CollectionUtils.isNotEmpty(errors)) {
                return st.withErrors(errors);
            }
            st.getCache().put(PAR_DIR_TO_SAVE_NEW_CARD_TO, dirStr);
            switch (cardDto) {
                case CardDto.FillGaps dto -> st.getCache().put(PAR_CARD_FILL_GAPS_LANG, dto.getLang());
            }
            cards.saveCard(new File(dir, makeFileName(card)), card);
            return st.withCardParams(clearParams(cardDto));
        } catch (Exception ex) {
            return st.withErrors(List.of(ex.getMessage()));
        }
    }

    private CardDto clearParams(CardDto dto) {
        return switch (dto) {
            case CardDto.FillGaps c -> c.withText("");
        };
    }

    private String makeFileName(Card card) {
        String baseName = UUID.randomUUID().toString().replace("-", "_");
        String extension = switch (card) {
            case Card.FillGaps _ -> CARD_FILL_GAPS_FILE_EXTENSION;
        };
        return baseName + extension;
    }

    private HtmlElem rndCard(NewCardState st) {
        CardDto cardParams = st.getCardParams();
        return switch (cardParams) {
            case CardDto.FillGaps dto -> rndCardFillGaps(st, dto);
        };
    }

    private HtmlElem rndCardFillGaps(NewCardState st, CardDto.FillGaps card) {
        return table(List.of(
            List.of(
                text("Language"),
                rndAvailableLanguages(st.getSettings(), card.getLang())
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

    private HtmlElem rndAvailableLanguages(Settings settings, String selectedLang) {
        return select(PAR_CARD_FILL_GAPS_LANG, selectedLang,
            settings.getLanguages().stream()
                .map(lang -> Pair.of(lang, text(lang)))
                .toList()
        );
    }

    private HtmlElem rndCardType(NewCardState state) {
        return table(List.of(List.of(
            text("Card type"),
            select(
                PAR_DIR_TO_SAVE_NEW_CARD_TO,
                state.getCardParams().getType().getDisplayName(),
                Arrays.stream(CardType.values())
                    .map(cardType -> Pair.of(cardType.getCode(), text(cardType.getDisplayName())))
                    .toList()
            )
        )));
    }

    private HtmlElem rndDirSelector(NewCardState state) {
        return table(List.of(List.of(
            text("Directory"),
            select(
                PAR_DIR_TO_SAVE_NEW_CARD_TO,
                state.getDir(),
                state.getSettings().getDirectoriesWithCards().stream()
                    .map(dir -> Pair.of(dir, text(dir)))
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

    private String getDir(RequestParams params, Settings settings, Cache cache) {
        if (params.hasParam(PAR_DIR_TO_SAVE_NEW_CARD_TO)) {
            return params.getParam(PAR_DIR_TO_SAVE_NEW_CARD_TO);
        } else {
            return cache.getStr(PAR_DIR_TO_SAVE_NEW_CARD_TO, getDefaultDir(settings));
        }
    }

    private String getDefaultDir(Settings settings) {
        return settings.getDirectoriesWithCards().getFirst();
    }

    private CardDto makeCardParams(RequestParams params, Settings settings, Cache cache) {
        if (params.hasParam(PAR_CARD_TYPE)) {
            return switch (CardType.fromCode(params.getParam(PAR_CARD_TYPE))) {
                case FILL_GAPS -> makeFillGapsCardParams(params, settings, cache);
            };
        } else {
            return makeFillGapsCardParams(params, settings, cache);
        }
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
}
