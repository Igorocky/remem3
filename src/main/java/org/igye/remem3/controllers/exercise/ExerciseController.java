package org.igye.remem3.controllers.exercise;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.igye.remem3.app.App;
import org.igye.remem3.app.Cache;
import org.igye.remem3.app.Settings;
import org.igye.remem3.app.impl.CacheImpl;
import org.igye.remem3.app.impl.SettingsImpl;
import org.igye.remem3.controllers.components.DirSelectorCmp;
import org.igye.remem3.controllers.components.impl.DirSelectorCmpImpl;
import org.igye.remem3.html.HtmlBuilder;
import org.igye.remem3.html.HtmlTag;
import org.igye.remem3.utils.Utils;
import org.igye.remem3.utils.web.RequestParams;
import org.igye.remem3.utils.web.impl.RequestParamsImpl;
import org.igye.remem3.web.StatefulWebController;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

@RequiredArgsConstructor
public class ExerciseController extends HtmlBuilder
    implements StatefulWebController<ExerciseState, Supplier<ExerciseState>> {

    private static final String PAR_DIR_TO_READ_TASKS_FROM = "PAR_DIR_TO_READ_TASKS_FROM";

    private final App app;
    private final Utils utils;

    @Override
    public String getPath() {
        return "exercise";
    }

    @Override
    public ExerciseState loadState(HttpServletRequest req) {
        try {
            app.reloadProperties();
            Settings settings = SettingsImpl.load(app);
            Cache cache = CacheImpl.load(utils, settings);
            RequestParams params = new RequestParamsImpl(req);
            DirSelectorCmp dirSelector = new DirSelectorCmpImpl(settings, cache, params, PAR_DIR_TO_READ_TASKS_FROM);
            return ExerciseState.builder()
                .dirSelector(dirSelector)
                .build();
        } catch (Exception ex) {
            return ExerciseState.builder()
                .errors(List.of(ex.getMessage()))
                .build();
        }
    }

    @Override
    public Optional<Supplier<ExerciseState>> decodeAction(HttpServletRequest req, ExerciseState state) {
        if (CollectionUtils.isNotEmpty(state.getErrors())) {
            return Optional.empty();
        }
        return Optional.empty();
    }

    @Override
    public ExerciseState updateState(ExerciseState state, Supplier<ExerciseState> action) {
        if (CollectionUtils.isNotEmpty(state.getErrors())) {
            return state;
        }
        return action.get();
    }

    @Override
    public void saveState(ExerciseState state) {

    }

    @Override
    public String renderState(ExerciseState st) {
        return simplePageWithTitle("Exercise",
            form(
                rndDirSelector(st)
            )
        ).toString();
    }

    private HtmlTag rndDirSelector(ExerciseState st) {
        return table(List.of(List.of(
            text("Directory"),
            frag(st.getDirSelector().render())
        )));
    }
}
