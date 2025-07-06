package org.igye.remem3.controllers.newcard;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.igye.remem3.app.App;
import org.igye.remem3.app.Cache;
import org.igye.remem3.app.Settings;
import org.igye.remem3.app.impl.CacheImpl;
import org.igye.remem3.app.impl.SettingsImpl;
import org.igye.remem3.html.HtmlBuilder;
import org.igye.remem3.html.HtmlElem;
import org.igye.remem3.utils.Exn;
import org.igye.remem3.utils.Utils;
import org.igye.remem3.utils.web.RequestParams;
import org.igye.remem3.utils.web.impl.RequestParamsImpl;
import org.igye.remem3.web.StatefulWebController;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static org.igye.remem3.app.impl.SettingsImpl.PROP_DIRECTORIES_WITH_CARDS;
import static org.igye.remem3.app.impl.SettingsImpl.PROP_LANGUAGES;

@RequiredArgsConstructor
public class NewCardController extends HtmlBuilder
    implements StatefulWebController<NewCardState, Supplier<NewCardState>> {

    private static final String PAR_DIR_TO_SAVE_NEW_CARD_TO = "PAR_DIR_TO_SAVE_NEW_CARD_TO";
    private static final String PAR_CARD_TYPE = "PAR_CARD_TYPE";
    public static final String CARD_TYPE_FILL_GAPS = "fill_gaps";
    private static final String PAR_CARD_FILL_GAPS_LANG = "PAR_CARD_FILL_GAPS_LANG";
    private static final String PAR_CARD_FILL_GAPS_TEXT = "PAR_CARD_FILL_GAPS_TEXT";
    private static final String PAR_CARD_FILL_GAPS_NOTES = "PAR_CARD_FILL_GAPS_NOTES";

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
    public String renderState(NewCardState state) {
        return simplePageWithTitle("Add new card",
            rndErrors(state.getErrors()),
            h3(text("Add new card")),
            form(
                rndDirSelector(state),
                rndCardType(state)
            )
        ).toString();
    }

    private HtmlElem rndCardType(NewCardState state) {
        return table(List.of(List.of(
            text("Card type"),
            select(
                PAR_DIR_TO_SAVE_NEW_CARD_TO,
                state.getCardParams().getCardType(),
                List.of(
                    Pair.of(CARD_TYPE_FILL_GAPS, text(CARD_TYPE_FILL_GAPS))
                )
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
        return h("div", Map.of("style", "color:red;"),
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
        List<String> directoriesWithCards = settings.getDirectoriesWithCards();
        if (CollectionUtils.isEmpty(directoriesWithCards)) {
            throw new Exn(String.format("Property '%s' is empty", PROP_DIRECTORIES_WITH_CARDS));
        }
        return directoriesWithCards.getFirst();
    }

    private CardDto makeCardParams(RequestParams params, Settings settings, Cache cache) {
        if (params.hasParam(PAR_CARD_TYPE)) {
            String cardType = params.getParam(PAR_CARD_TYPE);
            return switch (cardType) {
                case CARD_TYPE_FILL_GAPS -> makeFillGapsCardParams(params, settings, cache);
                default -> throw new Exn(String.format("Unexpected type of card %s", cardType));
            };
        } else {
            return makeFillGapsCardParams(params, settings, cache);
        }
    }

    private CardFillGapsDto makeFillGapsCardParams(RequestParams params, Settings settings, Cache cache) {
        if (CollectionUtils.isEmpty(settings.getLanguages())) {
            throw new Exn(String.format("Property '%s' is empty", PROP_LANGUAGES));
        }
        return CardFillGapsDto.builder()
            .lang(params.getParam(
                PAR_CARD_FILL_GAPS_LANG,
                cache.getStr(PAR_CARD_FILL_GAPS_LANG, settings.getLanguages().getFirst())
            ))
            .text(params.getParam(PAR_CARD_FILL_GAPS_TEXT, ""))
            .notes(params.getParam(PAR_CARD_FILL_GAPS_NOTES, ""))
            .build();
    }
}
