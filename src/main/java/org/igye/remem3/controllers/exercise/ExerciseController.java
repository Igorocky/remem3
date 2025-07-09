package org.igye.remem3.controllers.exercise;

import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.igye.remem3.app.App;
import org.igye.remem3.app.Cache;
import org.igye.remem3.app.Cards;
import org.igye.remem3.app.RepeatStrategy;
import org.igye.remem3.app.Settings;
import org.igye.remem3.app.TaskTypeMatcher;
import org.igye.remem3.app.dto.Task;
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
import org.igye.remem3.web.RequestParams;
import org.igye.remem3.web.StatefulWebController;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
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
    private static final String ACT_START_EXERCISE = "ACT_START_EXERCISE";
    private static final String ACT_CANCEL_EXERCISE = "ACT_CANCEL_EXERCISE";
    private static final String ACT_TOGGLE_SHOW_EXERCISE_PARAMS = "ACT_TOGGLE_SHOW_EXERCISE_PARAMS";
    private static final String PAR_SHOW_EXERCISE_PARAMS = "PAR_SHOW_EXERCISE_PARAMS";

    private final App app;
    private final Utils utils;
    private ExerciseState.Started startedState;

    @Override
    public String getPath() {
        return "exercise";
    }

    @Override
    public ExerciseState loadState(RequestParams params) {
        try {
            app.reloadProperties();
            Settings settings = SettingsImpl.load(app);
            Cache cache = CacheImpl.load(utils, settings);
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
                return makeSetParamsState(settings, cache, params);
            } catch (Exception ex2) {
                return ExerciseState.SetParams.builder()
                    .errors(List.of(ex1.getMessage(), ex2.getMessage()))
                    .build();
            }
        }
    }

    @Override
    public Optional<ExerciseAction> decodeAction(RequestParams params, ExerciseState state) {
        if (CollectionUtils.isNotEmpty(state.getErrors())) {
            return Optional.empty();
        }
        return switch (state) {
            case ExerciseState.SetParams _ -> {
                if (params.hasParam(ACT_START_EXERCISE)) {
                    yield Optional.of(new ExerciseAction.StartExercise());
                }
                yield Optional.empty();
            }
            case ExerciseState.Started _ -> {
                if (params.hasParam(ACT_CANCEL_EXERCISE)) {
                    yield Optional.of(new ExerciseAction.CancelExercise());
                }
                if (params.hasParam(ACT_TOGGLE_SHOW_EXERCISE_PARAMS)) {
                    yield Optional.of(new ExerciseAction.ToggleShowExerciseParams());
                }
                yield Optional.empty();
            }
        };
    }

    @Override
    public ExerciseState updateState(ExerciseState state, ExerciseAction action) {
        return switch (action) {
            case ExerciseAction.StartExercise _ -> actStartExercise((ExerciseState.SetParams) state);
            case ExerciseAction.CancelExercise _ -> actCancelExercise((ExerciseState.Started) state);
            case ExerciseAction.ToggleShowExerciseParams _ -> actToggleShowParams((ExerciseState.Started) state);
        };
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

    private ExerciseState actToggleShowParams(ExerciseState.Started st) {
        boolean newShowParams = !st.isShowParams();
        st.getCache().put(PAR_SHOW_EXERCISE_PARAMS, newShowParams);
        return st.withShowParams(newShowParams);
    }

    private ExerciseState actStartExercise(ExerciseState.SetParams st) {
        Set<String> taskTypes = st.getTaskTypes().stream()
            .filter(Pair::getRight)
            .map(Pair::getLeft)
            .map(TaskType::getCode)
            .collect(Collectors.toSet());
        TaskTypeMatcher taskTypeMatcher = TaskTypeMatcher.fromList(taskTypes);
        File selectedDir = st.getDirSelector().getSelectedDirectory();
        List<Task> tasks = st.getCards().loadAllCards(selectedDir).stream()
            .flatMap(card -> card.getTasks().stream())
            .filter(task -> taskTypeMatcher.matches(task.getTaskType()))
            .toList();
        if (tasks.isEmpty()) {
            return st.withErrors(List.of("There are no tasks."));
        }
        RepeatStrategy repeatStrategy = st.getRepeatStrategyCmp().makeRepeatStrategy(tasks);
        return ExerciseState.Started.builder()
            .settings(st.getSettings())
            .cache(st.getCache())
            .cards(st.getCards())
            .repeatStrategyCmp(st.getRepeatStrategyCmp())
            .dir(selectedDir.getAbsolutePath())
            .taskTypes(taskTypes)
            .repeatStrategy(repeatStrategy)
            .showParams(st.getCache().getBool(PAR_SHOW_EXERCISE_PARAMS, false))
            .nextTasks(repeatStrategy.getNextTasks())
            .build();
    }

    private ExerciseState actCancelExercise(ExerciseState.Started st) {
        DirSelectorCmpImpl dirSelector = new DirSelectorCmpImpl(
            st.getSettings(), st.getCache(), st.getDir(), PAR_DIR_TO_READ_TASKS_FROM
        );
        return ExerciseState.SetParams.builder()
            .settings(st.getSettings())
            .cache(st.getCache())
            .cards(st.getCards())
            .dirSelector(dirSelector)
            .taskTypes(getTaskTypes(
                getAvailableTaskTypes(st.getCards(), dirSelector.getSelectedDirectory()),
                new HashSet<>(st.getTaskTypes())
            ))
            .repeatStrategyCmp(st.getRepeatStrategyCmp())
            .build();
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
            h4(text("Select exercise")),
            rndDirSelector(st),
            rndTaskTypes(st),
            h("br"),
            st.getRepeatStrategyCmp().render(),
            h("br"),
            inpSubmit(ACT_START_EXERCISE, "Start")
        );
    }

    private HtmlElem rndExercise(ExerciseState.Started st) {
        HtmlElem params;
        if (st.isShowParams()) {
            String taskTypesStr = st.getTaskTypes().isEmpty()
                ? "All available in the directory"
                : st.getTaskTypes().stream().sorted().collect(Collectors.joining(", "));
            params = frag(
                div("", text(String.format("Directory: %s", st.getDir()))),
                div("", text(String.format("Task types: %s", taskTypesStr))),
                div("", st.getRepeatStrategy().renderStats())
            );
        } else {
            params = null;
        }
        boolean completed = st.getNextTasks().isEmpty();
        return frag(
            h4(text("Exercise")),
            inpSubmit(ACT_TOGGLE_SHOW_EXERCISE_PARAMS, st.isShowParams() ? "Hide parameters" : "Show parameters"),
            params,
            h("hr"),
            inpSubmit(ACT_CANCEL_EXERCISE, completed ? "Done" : "Cancel")
        );
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
        Cards cards = new CardsImpl(utils, settings);
        return ExerciseState.SetParams.builder()
            .settings(settings)
            .cache(cache)
            .cards(cards)
            .dirSelector(dirSelector)
            .taskTypes(getTaskTypes(getAvailableTaskTypes(cards, dirSelector.getSelectedDirectory()), params))
            .repeatStrategyCmp(new RepeatStrategyCmpImpl("PAR_REPEAT_STRATEGY", params))
            .build();
    }

    private static List<TaskType> getAvailableTaskTypes(Cards cards, File dir) {
        return cards.loadAllCards(dir).stream()
            .flatMap(card -> card.getTaskTypes().stream())
            .distinct()
            .sorted(Comparator.comparing(TaskType::getCode))
            .toList();
    }

    private List<Pair<TaskType, Boolean>> getTaskTypes(List<TaskType> availableTaskTypes, RequestParams params) {
        Set<String> checked = Arrays.stream(params.getParams(PAR_TASK_TYPE)).collect(Collectors.toSet());
        return getTaskTypes(availableTaskTypes, checked);
    }

    private List<Pair<TaskType, Boolean>> getTaskTypes(List<TaskType> availableTaskTypes, Set<String> checked) {
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
