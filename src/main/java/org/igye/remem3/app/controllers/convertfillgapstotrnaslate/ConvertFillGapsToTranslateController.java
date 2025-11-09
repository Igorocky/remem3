package org.igye.remem3.app.controllers.convertfillgapstotrnaslate;

import lombok.RequiredArgsConstructor;
import org.igye.remem3.app.Cache;
import org.igye.remem3.app.CardUtils;
import org.igye.remem3.app.Settings;
import org.igye.remem3.app.controllers.components.DirSelectorCmp;
import org.igye.remem3.app.controllers.components.impl.DirSelectorCmpImpl;
import org.igye.remem3.html.HtmlBuilder;
import org.igye.remem3.html.HtmlTag;
import org.igye.remem3.web.RequestParams;
import org.igye.remem3.web.StatefulWebController;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

@RequiredArgsConstructor
public class ConvertFillGapsToTranslateController extends HtmlBuilder
    implements StatefulWebController<State, Supplier<State>> {

    private static final String PAR_DIR_TO_CONVERT_TASKS_IN = "PAR_DIR_TO_CONVERT_TASKS_IN";

    private final Settings settings;
    private final Cache cache;
    private final CardUtils cardUtils;

    @Override
    public String getId() {
        return "convert_fill_gaps_to_translate";
    }

    @Override
    public String getTitle() {
        return "Convert FillGaps cards to Translate cards";
    }

    @Override
    public State loadState(RequestParams params) {
        DirSelectorCmp dirSelector = new DirSelectorCmpImpl(settings, cache, params, PAR_DIR_TO_CONVERT_TASKS_IN);
        return State.builder()
            .dirSelector(dirSelector)
            .build();
    }

    @Override
    public Optional<Supplier<State>> decodeAction(RequestParams params, State state) {
        return Optional.empty();
    }

    @Override
    public State updateState(State state, Supplier<State> action) {
        return action.get();
    }

    @Override
    public void saveState(State state) {
    }

    @Override
    public String renderState(State st) {
        return simplePageWithTitle("Convert FillGaps to Translate",
            h4(text("Convert FillGaps to Translate")),
            form(
                rndDirSelector(st.getDirSelector())
            )
        ).toString();
    }

    private HtmlTag rndDirSelector(DirSelectorCmp dirSelector) {
        return table(List.of(List.of(
            text("Directory"),
            frag(dirSelector.render())
        )));
    }
}
