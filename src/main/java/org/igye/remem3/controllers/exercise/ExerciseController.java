package org.igye.remem3.controllers.exercise;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.igye.remem3.app.App;
import org.igye.remem3.app.Cache;
import org.igye.remem3.app.Settings;
import org.igye.remem3.app.dto.TaskType;
import org.igye.remem3.app.impl.CacheImpl;
import org.igye.remem3.app.impl.CardsImpl;
import org.igye.remem3.app.impl.SettingsImpl;
import org.igye.remem3.controllers.components.DirSelectorCmp;
import org.igye.remem3.controllers.components.impl.DirSelectorCmpImpl;
import org.igye.remem3.controllers.components.impl.RepeatStrategyCmpImpl;
import org.igye.remem3.html.HtmlBuilder;
import org.igye.remem3.html.HtmlElem;
import org.igye.remem3.html.HtmlTag;
import org.igye.remem3.utils.Utils;
import org.igye.remem3.utils.web.RequestParams;
import org.igye.remem3.utils.web.impl.RequestParamsImpl;
import org.igye.remem3.web.StatefulWebController;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class ExerciseController extends HtmlBuilder
    implements StatefulWebController<ExerciseState, ExerciseAction> {

    private static final String PAR_EXERCISE_STAGE = "PAR_EXERCISE_STAGE";
    private static final String PAR_DIR_TO_READ_TASKS_FROM = "PAR_DIR_TO_READ_TASKS_FROM";
    private static final String PAR_TASK_TYPE = "PAR_TASK_TYPE";

    private final App app;
    private final Utils utils;
    private ExerciseState.Started startedState;

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
            ExerciseStage stage = params.hasParam(PAR_EXERCISE_STAGE)
                ? ExerciseStage.valueOf(params.getParam(PAR_EXERCISE_STAGE))
                : ExerciseStage.SET_PARAMS;
            return switch (stage) {
                case SET_PARAMS -> makeSetParamsState(settings, cache, params);
                case STARTED -> startedState != null ? startedState : makeSetParamsState(settings, cache, params);
            };
        } catch (Exception ex1) {
            try {
                Settings settings = SettingsImpl.load(app);
                Cache cache = CacheImpl.load(utils, settings);
                RequestParams params = new RequestParamsImpl(req);
                return makeSetParamsState(settings, cache, params);
            } catch (Exception ex2) {
                return ExerciseState.SetParams.builder()
                    .errors(List.of(ex1.getMessage(), ex2.getMessage()))
                    .build();
            }
        }
    }

    @Override
    public Optional<ExerciseAction> decodeAction(HttpServletRequest req, ExerciseState state) {
        if (CollectionUtils.isNotEmpty(state.getErrors())) {
            return Optional.empty();
        }
        return switch (state) {
            case ExerciseState.SetParams st -> Optional.empty();
            case ExerciseState.Started st -> Optional.empty();
        };
    }

    @Override
    public ExerciseState updateState(ExerciseState state, ExerciseAction action) {
        return state;
    }

    @Override
    public void saveState(ExerciseState state) {
        switch (state) {
            case ExerciseState.SetParams _ -> this.startedState = null;
            case ExerciseState.Started st -> this.startedState = st;
        }
    }

    @Override
    public String renderState(ExerciseState state) {
        return simplePageWithTitle("Exercise",
            rndErrors(state.getErrors()),
            form(
                rndStage(state),
                switch (state) {
                    case ExerciseState.SetParams st -> rndParams(st);
                    case ExerciseState.Started st -> rndExercise(st);
                }
            )
        ).toString();
    }

    private HtmlElem rndStage(ExerciseState state) {
        ExerciseStage stage = switch (state) {
            case ExerciseState.SetParams _ -> ExerciseStage.SET_PARAMS;
            case ExerciseState.Started _ -> ExerciseStage.STARTED;
        };
        return inpHidden(PAR_EXERCISE_STAGE, stage.toString());
    }

    private HtmlElem rndParams(ExerciseState.SetParams st) {
        return frag(
            rndDirSelector(st),
            rndTaskTypes(st),
            h("br"),
            st.getRepeatStrategyCmp().render()
        );
    }

    private HtmlElem rndExercise(ExerciseState.Started st) {
        return null;
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

    private ExerciseState.SetParams makeSetParamsState(Settings settings, Cache cache, RequestParams params) {
        DirSelectorCmp dirSelector = new DirSelectorCmpImpl(settings, cache, params, PAR_DIR_TO_READ_TASKS_FROM);
        CardsImpl cards = new CardsImpl(utils, settings);
        List<TaskType> availableTaskTypes = cards.loadAllCards(dirSelector.getSelectedDirectory()).stream()
            .flatMap(card -> card.getTaskTypes().stream())
            .distinct()
            .sorted(Comparator.comparing(TaskType::getCode))
            .toList();
        return ExerciseState.SetParams.builder()
            .cards(cards)
            .dirSelector(dirSelector)
            .taskTypes(getTaskTypes(availableTaskTypes, params))
            .repeatStrategyCmp(new RepeatStrategyCmpImpl("PAR_REPEAT_STRATEGY", params))
            .build();
    }

    private List<Pair<TaskType, Boolean>> getTaskTypes(List<TaskType> availableTaskTypes, RequestParams params) {
        Set<String> checked = Arrays.stream(params.getParams(PAR_TASK_TYPE)).collect(Collectors.toSet());
        return availableTaskTypes.stream()
            .map(typ -> Pair.of(typ, checked.contains(typ.getCode())))
            .toList();
    }

    private HtmlElem rndTaskTypes(ExerciseState.SetParams st) {
        return frag(
            h5(text("Task types")),
            table(
                st.getTaskTypes().stream()
                    .map(typ -> List.of(
                        inpCheckbox(PAR_TASK_TYPE, typ.getLeft().getCode(), typ.getRight()),
                        text(typ.getLeft().getCode())
                    ))
                    .toList()
            )
        );
    }

    private HtmlTag rndDirSelector(ExerciseState.SetParams st) {
        return table(List.of(List.of(
            text("Directory"),
            frag(st.getDirSelector().render())
        )));
    }
}
