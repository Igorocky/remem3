package org.igye.remem3.controllers.exercise;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.igye.remem3.app.App;
import org.igye.remem3.app.Cache;
import org.igye.remem3.app.Settings;
import org.igye.remem3.app.impl.CacheImpl;
import org.igye.remem3.app.impl.SettingsImpl;
import org.igye.remem3.html.HtmlBuilder;
import org.igye.remem3.html.HtmlElem;
import org.igye.remem3.utils.Utils;
import org.igye.remem3.utils.web.RequestParams;
import org.igye.remem3.utils.web.impl.RequestParamsImpl;
import org.igye.remem3.web.StatefulWebController;

import java.util.Optional;
import java.util.function.Supplier;

@RequiredArgsConstructor
public class ExerciseController extends HtmlBuilder
    implements StatefulWebController<ExerciseState, Supplier<ExerciseState>> {


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
            return null;
        } catch (Exception ex) {
            return null;
        }
    }

    @Override
    public Optional<Supplier<ExerciseState>> decodeAction(HttpServletRequest req, ExerciseState state) {
        return Optional.empty();
    }

    @Override
    public ExerciseState updateState(ExerciseState state, Supplier<ExerciseState> action) {
        return action.get();
    }

    @Override
    public void saveState(ExerciseState state) {

    }

    @Override
    public String renderState(ExerciseState st) {
        return simplePageWithTitle("Exercise",
            form(
                (HtmlElem) null
            )
        ).toString();
    }
}
