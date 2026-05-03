package org.igye.remem3.app.controllers.exercise;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.igye.remem3.app.Cache;
import org.igye.remem3.app.CardUtils;
import org.igye.remem3.app.Settings;
import org.igye.remem3.app.TaskTypeMatcher;
import org.igye.remem3.app.controllers.components.DirSelectorCmp;
import org.igye.remem3.app.controllers.components.impl.DirSelectorCmpImpl;
import org.igye.remem3.app.controllers.components.impl.RepeatStrategyCmpImpl;
import org.igye.remem3.app.dto.Card;
import org.igye.remem3.app.dto.Task;
import org.igye.remem3.app.dto.TaskType;
import org.igye.remem3.app.impl.CardUtilsImpl;
import org.igye.remem3.app.repeatstrategy.RepeatStrategy;
import org.igye.remem3.app.repeatstrategy.impl.RepeatStrategyBuckets;
import org.igye.remem3.app.task.TaskResult;
import org.igye.remem3.app.task.TaskState;
import org.igye.remem3.app.task.impl.TaskStateFillGaps;
import org.igye.remem3.app.task.impl.TaskStateTranslate;
import org.igye.remem3.html.HtmlBuilder;
import org.igye.remem3.html.HtmlElem;
import org.igye.remem3.html.HtmlTag;
import org.igye.remem3.utils.Exn;
import org.igye.remem3.utils.Utils;
import org.igye.remem3.web.RequestParams;
import org.igye.remem3.web.StatefulWebController;
import org.igye.remem3.web.impl.RequestParamsImpl;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.FileReader;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;

@RequiredArgsConstructor
public class ExerciseController extends HtmlBuilder
    implements StatefulWebController<ExerciseState, Supplier<? extends ExerciseState>> {

    private static final String PAR_EXERCISE_STAGE = "PAR_EXERCISE_STAGE";
    private static final String PAR_EXERCISE_CONFIG = "PAR_EXERCISE_CONFIG";
    private static final String PAR_DIR_TO_READ_TASKS_FROM = "PAR_DIR_TO_READ_TASKS_FROM";
    private static final String PAR_TASK_TYPE = "PAR_TASK_TYPE";
    private static final String PAR_SHOW_EXERCISE_PARAMS = "PAR_SHOW_EXERCISE_PARAMS";
    private static final String PAR_SHOW_DAILY_UNIQUE_COUNT = "PAR_SHOW_DAILY_UNIQUE_COUNT";
    private static final String ACT_SHOW_PROPERTIES = "ACT_SHOW_PROPERTIES";
    private static final String ACT_CUSTOMIZE_EXERCISE = "ACT_CUSTOMIZE_EXERCISE";
    private static final String ACT_START_EXERCISE = "ACT_START_EXERCISE";
    private static final String ACT_CANCEL_EXERCISE = "ACT_CANCEL_EXERCISE";
    private static final String ACT_REFRESH_EXERCISE = "ACT_REFRESH_EXERCISE";
    private static final String ACT_TOGGLE_SHOW_EXERCISE_PARAMS = "ACT_TOGGLE_SHOW_EXERCISE_PARAMS";
    private static final String ACT_TOGGLE_SHOW_LESS_MORE_EXERCISE_PARAMS = "ACT_TOGGLE_SHOW_LESS_MORE_EXERCISE_PARAMS";
    private static final String ACT_TOGGLE_SHOW_DAILY_UNIQUE_COUNT = "ACT_TOGGLE_SHOW_DAILY_UNIQUE_COUNT";
    private static final String ACT_SKIP_TASK = "ACT_SKIP_TASK";
    private static final String ACT_COPY_CARD_PATH_TO_CLIPBOARD = "ACT_COPY_CARD_PATH_TO_CLIPBOARD";
    private static final String ACT_OPEN_CARD = "ACT_OPEN_CARD";
    private static final String PROP_DIRECTORY = "directory";
    private static final String PROP_TASKS = "tasks";

    private final Clock clock;
    private final Settings settings;
    private final Cache cache;
    private final Utils utils;

    private volatile ExerciseState state;

    @Override
    public String getId() {
        return "exercise";
    }

    @Override
    public String getTitle() {
        return "Exercises";
    }

    @Override
    public ExerciseState loadState(RequestParams params) {
        ExerciseState prevState = params.hasParam(PAR_EXERCISE_STAGE) ? state : null;
        try {
            ExerciseStage stage = params.getParamOpt(PAR_EXERCISE_STAGE)
                .map(ExerciseStage::valueOf)
                .orElse(ExerciseStage.SET_PARAMS);
            return switch (stage) {
                case SET_PARAMS -> makeSetParamsState(settings, cache, params, prevState);
                case STARTED -> switch (prevState) {
                    case null -> makeSetParamsState(settings, cache, params, null);
                    case ExerciseState.SetParams _ -> makeSetParamsState(settings, cache, params, null);
                    case ExerciseState.Started st -> st.withCardPathCopied(false);
                };
            };
        } catch (Exception ex1) {
            try {
                return makeSetParamsState(settings, cache, params, prevState);
            } catch (Exception ex2) {
                return ExerciseState.SetParams.builder()
                    .errors(Stream.of(ex1.getMessage(), ex2.getMessage()).distinct().toList())
                    .build();
            }
        }
    }

    @Override
    public Optional<Supplier<? extends ExerciseState>> decodeAction(RequestParams params, ExerciseState state) {
        if (CollectionUtils.isNotEmpty(state.getErrors())) {
            return Optional.empty();
        }
        return switch (state) {
            case ExerciseState.SetParams st -> {
                if (params.hasParam(ACT_START_EXERCISE)) {
                    yield Optional.of(() -> actStartExercise(st));
                }
                if (params.hasParam(ACT_SHOW_PROPERTIES)) {
                    yield Optional.of(() -> st.withShowProperties(true));
                }
                if (params.hasParam(ACT_CUSTOMIZE_EXERCISE)) {
                    yield Optional.of(() -> actCustomizeExercise(st));
                }
                yield Optional.empty();
            }
            case ExerciseState.Started st -> {
                if (params.hasParam(ACT_CANCEL_EXERCISE)) {
                    yield Optional.of(this::actCancelExercise);
                }
                if (params.hasParam(ACT_TOGGLE_SHOW_EXERCISE_PARAMS)) {
                    yield Optional.of(() -> actToggleShowParams(st));
                }
                if (params.hasParam(ACT_TOGGLE_SHOW_LESS_MORE_EXERCISE_PARAMS)) {
                    yield Optional.of(() -> actToggleShowLessMoreParams(st));
                }
                if (params.hasParam(ACT_TOGGLE_SHOW_DAILY_UNIQUE_COUNT)) {
                    yield Optional.of(() -> actToggleShowDailyUniqueCount(st));
                }
                if (params.hasParam(ACT_SKIP_TASK) || params.hasParam(ACT_REFRESH_EXERCISE)) {
                    yield Optional.of(() -> actGoToNextTask(st));
                }
                if (params.hasParam(ACT_COPY_CARD_PATH_TO_CLIPBOARD)) {
                    yield Optional.of(() -> actCopyCardPathToClipboard(st));
                }
                if (params.hasParam(ACT_OPEN_CARD)) {
                    yield Optional.of(() -> actOpenCard(st));
                }
                if (st.getTaskState().isPresent()) {
                    yield Optional.of(() ->
                        actProcessTaskResults(st, st.getTaskState().get().processUserInput(params))
                    );
                }
                yield Optional.empty();
            }
        };
    }

    @Override
    public ExerciseState updateState(ExerciseState state, Supplier<? extends ExerciseState> action) {
        return action.get();
    }

    @Override
    public void saveState(ExerciseState state) {
        this.state = state;
    }

    @Override
    public String renderState(ExerciseState state) {
        return simplePageWithTitle("Exercise",
            CollectionUtils.isNotEmpty(state.getErrors()) ? rndErrors(state.getErrors()) : form(
                rndStage(state),
                switch (state) {
                    case ExerciseState.SetParams st -> rndParams(st);
                    case ExerciseState.Started st -> rndExercise(st);
                }
            )
        ).toString();
    }

    @SneakyThrows
    private ExerciseState actProcessTaskResults(ExerciseState.Started st, TaskResult taskResult) {
        if (taskResult.getHistRec().isPresent()) {
            File file = getCurrentCardFileExn(st);
            st.getCardUtils().appendHistRecToFile(
                file,
                taskResult.getHistRec().get().withStrategy(st.getRepeatStrategyCmp().getStrategyType())
            );
            getCurrentCardExn(st).copyFrom(st.getCardUtils().loadCard(file));
        }
        if (taskResult.isCompleted()) {
            return actGoToNextTask(st);
        }
        return st;
    }

    private ExerciseState actCustomizeExercise(ExerciseState.SetParams st) {
        st.getRepeatStrategyCmp().setIsReadonly(false);
        return st
            .withDirSelector(
                new DirSelectorCmpImpl(
                    settings, cache, PAR_DIR_TO_READ_TASKS_FROM, false, st.getDirSelector().getSelectedDirectory()
                )
            )
            .withConfig("");
    }

    private ExerciseState actCopyCardPathToClipboard(ExerciseState.Started st) {
        StringSelection stringSelection = new StringSelection(getCurrentCardFileExn(st).getAbsolutePath());
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, null);
        return st.withCardPathCopied(true);
    }

    @SneakyThrows
    private ExerciseState actOpenCard(ExerciseState.Started st) {
        new ProcessBuilder(
            st.getSettings().getCardEditor(),
            getCurrentCardFileExn(st).getAbsolutePath()
        ).start();
        return st;
    }

    private ExerciseState.Started actGoToNextTask(ExerciseState.Started st) {
        getCurrentCard(st).ifPresent(card ->
            card.copyFrom(st.getCardUtils().loadCard(getCurrentCardFileExn(st)))
        );
        Optional<List<Task>> nextTasksOpt = st.getNextTasks().flatMap(tasks -> {
            if (tasks.size() < 2) {
                return st.getRepeatStrategy().getNextTasks().map(ExerciseController::getListOfBaseTasks);
            } else {
                List<Task> tail = new ArrayList<>(tasks);
                tail.removeFirst();
                return Optional.of(tail);
            }
        });
        if (nextTasksOpt.isEmpty() || nextTasksOpt.get().isEmpty()) {
            return st
                .withNextTasks(nextTasksOpt)
                .withTaskState(Optional.empty());
        }
        Task nextTask = nextTasksOpt.get().getFirst();
        return st
            .withNextTasks(nextTasksOpt)
            .withTaskState(makeTaskState(st.getCardUtils(), nextTask));
    }

    private static List<Task> getListOfBaseTasks(List<org.igye.remem3.app.repeatstrategy.Task> ts) {
        return ts.stream()
            .map(HasBaseTask.class::cast)
            .map(HasBaseTask::getBaseTask)
            .toList();
    }

    private Optional<Card> getCurrentCard(ExerciseState.Started st) {
        return st.getNextTasks()
            .flatMap(nextTasks -> nextTasks.isEmpty() ? Optional.empty() : Optional.of(nextTasks.getFirst()))
            .map(Task::getCard);
    }

    private Card getCurrentCardExn(ExerciseState.Started st) {
        return st.getNextTasks()
            .flatMap(nextTasks -> nextTasks.isEmpty() ? Optional.empty() : Optional.of(nextTasks.getFirst()))
            .map(Task::getCard)
            .orElseThrow(() -> new Exn("Cannot get the card for the current task."));
    }

    private Optional<File> getCurrentCardFile(ExerciseState.Started st) {
        return getCurrentCard(st).flatMap(Card::getFile);
    }

    private File getCurrentCardFileExn(ExerciseState.Started st) {
        return getCurrentCardFile(st).orElseThrow(() -> new Exn("Cannot determine the file for the current task."));
    }

    private ExerciseState actToggleShowParams(ExerciseState.Started st) {
        ExerciseState.Started newState;
        if (st.getShowMoreParams().isEmpty()) {
            newState = st.withShowMoreParams(Optional.of(true));
        } else {
            newState = st.withShowMoreParams(Optional.empty());
        }
        cacheShowMoreParams(newState);
        return newState;
    }

    private ExerciseState actToggleShowLessMoreParams(ExerciseState.Started st) {
        ExerciseState.Started newState;
        if (st.getShowMoreParams().isEmpty()) {
            newState = st.withShowMoreParams(Optional.of(true));
        } else {
            newState = st.withShowMoreParams(Optional.of(!st.getShowMoreParams().get()));
        }
        cacheShowMoreParams(newState);
        return newState;
    }

    private ExerciseState actToggleShowDailyUniqueCount(ExerciseState.Started st) {
        ExerciseState.Started newState = st.withShowDailyUniqueCount(!st.isShowDailyUniqueCount());
        newState.getCache().put(PAR_SHOW_DAILY_UNIQUE_COUNT, newState.isShowDailyUniqueCount());
        return newState;
    }

    private void cacheShowMoreParams(ExerciseState.Started st) {
        st.getCache().put(PAR_SHOW_EXERCISE_PARAMS, st.getShowMoreParams().map(Object::toString).orElse(""));
    }

    private ExerciseState actStartExercise(ExerciseState.SetParams st) {
        Set<String> selectedTaskTypes = st.getTaskTypes().stream()
            .filter(Pair::getRight)
            .map(Pair::getLeft)
            .map(TaskType::getCode)
            .collect(Collectors.toSet());
        TaskTypeMatcher taskTypeMatcher = TaskTypeMatcher.fromList(selectedTaskTypes);
        File selectedDir = st.getDirSelector().getSelectedDirectory();
        List<Task> tasks = st.getCardUtils().loadAllCards(selectedDir).stream()
            .flatMap(card -> card.getTasks().stream())
            .filter(task -> taskTypeMatcher.matches(task.getTaskType()))
            .toList();
        if (tasks.isEmpty()) {
            return st.withErrors(List.of("There are no tasks."));
        }
        RepeatStrategy repeatStrategy = st.getRepeatStrategyCmp().makeRepeatStrategy(tasks);
        Optional<List<Task>> nextTasks = repeatStrategy.getNextTasks().map(ExerciseController::getListOfBaseTasks);
        Cache cache = st.getCache();
        cache.put(PAR_EXERCISE_CONFIG, st.getConfig());
        if (StringUtils.isBlank(st.getConfig())) {
            cache.put(PAR_DIR_TO_READ_TASKS_FROM, st.getDirSelector().getSelectedDirectoryStr());
            cache.put(PAR_TASK_TYPE, String.join(",", selectedTaskTypes));
            st.getRepeatStrategyCmp().cacheState();
        }
        return ExerciseState.Started.builder()
            .settings(st.getSettings())
            .cache(cache)
            .cardUtils(st.getCardUtils())
            .config(st.getConfig())
            .repeatStrategyCmp(st.getRepeatStrategyCmp())
            .dir(selectedDir.getAbsolutePath())
            .taskTypes(selectedTaskTypes)
            .repeatStrategy(repeatStrategy)
            .showMoreParams(parseShowMoreParams(cache.getStr(PAR_SHOW_EXERCISE_PARAMS, "false")))
            .showDailyUniqueCount(cache.getBool(PAR_SHOW_DAILY_UNIQUE_COUNT, false))
            .nextTasks(nextTasks)
            .taskState(nextTasks.flatMap(nt ->
                nt.isEmpty() ? Optional.empty() : makeTaskState(st.getCardUtils(), nt.getFirst())
            ))
            .build();
    }

    private Optional<Boolean> parseShowMoreParams(String str) {
        if (StringUtils.isBlank(str)) {
            return Optional.empty();
        } else {
            return utils.try_(
                () -> Boolean.parseBoolean(str)
            );
        }
    }

    private Optional<TaskState> makeTaskState(CardUtils cardUtils, Task task) {
        return switch (task.getTaskType()) {
            case TaskType.FillGaps t ->
                Optional.of(new TaskStateFillGaps(clock, utils, cardUtils, (Card.FillGaps) task.getCard(), t));
            case TaskType.Translate t ->
                Optional.of(new TaskStateTranslate(clock, utils, cardUtils, (Card.Translate) task.getCard(), t));
        };
    }

    private ExerciseState actCancelExercise() {
        return makeSetParamsState(settings, cache, RequestParamsImpl.empty(), null);
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
            h4(text("Select exercise"), rndExerciseConfigSelector(st)),
            rndDirSelector(st),
            rndTaskTypes(st),
            br(),
            st.getRepeatStrategyCmp().render(),
            br(),
            div(
                inpSubmit(ACT_START_EXERCISE, "Start")
                    .attr("style", format("background-color: %s;", GREEN))
                    .attr("autofocus", "").attr("class", "border-on-focus"),
                st.isShowProperties()
                    ? null
                    : inpSubmit(ACT_SHOW_PROPERTIES, "Show properties").attr("class", "border-on-focus")
            ),
            !st.isShowProperties() ? null : rndProperties(st)
        );
    }

    private HtmlElem rndExerciseConfigSelector(ExerciseState.SetParams st) {
        ArrayList<Pair<String, ? extends HtmlElem>> options = new ArrayList<>();
        options.add(Pair.of("", text("CUSTOM")));
        st.getSettings().getExercises().stream()
            .map(ex -> Pair.of(ex.getLeft(), text(ex.getLeft())))
            .forEach(options::add);
        return frag(
            select(PAR_EXERCISE_CONFIG, st.getConfig(), options).submitOnChange(),
            StringUtils.isNotBlank(st.getConfig()) ? inpSubmit(ACT_CUSTOMIZE_EXERCISE, "Customize") : null
        );
    }

    private HtmlElem rndProperties(ExerciseState.SetParams st) {
        List<Pair<String, String>> props = new ArrayList<>();
        props.add(Pair.of(PROP_DIRECTORY, st.getDirSelector().getSelectedDirectoryStr()));
        props.add(Pair.of(
            PROP_TASKS,
            st.getTaskTypes().stream()
                .filter(Pair::getRight)
                .map(Pair::getLeft)
                .map(TaskType::getCode)
                .collect(Collectors.joining(", "))
        ));
        props.addAll(st.getRepeatStrategyCmp().getProperties());
        return frag(
            hr(),
            frag(
                props.stream()
                    .map(prop -> div(text(prop.getLeft() + "=" + prop.getRight())))
                    .toList()
            ),
            hr()
        );
    }

    private HtmlElem rndExercise(ExerciseState.Started st) {
        HtmlElem params;
        Optional<String> cardPath = getCurrentCardFile(st).map(File::getAbsolutePath);
        if (st.getShowMoreParams().isPresent()) {
            Boolean historyUpdated = st.getTaskState().map(TaskState::isHistoryUpdated).orElse(false);
            if (st.getShowMoreParams().get()) {
                String taskTypesStr = st.getTaskTypes().isEmpty()
                    ? "All available in the directory"
                    : st.getTaskTypes().stream().sorted().collect(Collectors.joining(", "));
                params = frag(
                    div(text(String.format("Directory: %s", st.getDir()))),
                    div(text(String.format("Task types: %s", taskTypesStr))),
                    div(frag(
                        text(String.format("Current card: %s ", cardPath.orElse("not available"))),
                        cardPath.isEmpty() ? null : frag(
                            inpSubmit(ACT_COPY_CARD_PATH_TO_CLIPBOARD, st.isCardPathCopied() ? "copied" : "copy path"),
                            inpSubmit(ACT_OPEN_CARD, "open")
                        )
                    )),
                    cardPath
                        .map(_ -> div(text(String.format("History updated: %s", historyUpdated ? "Yes" : "No"))))
                        .orElse(null),
                    br(),
                    div(text(String.format("Repeat strategy: %s", st.getRepeatStrategyCmp().getStrategyType()))),
                    div(st.getRepeatStrategy().renderMoreParams(historyUpdated))
                );
            } else {
                params = frag(
                    text(String.format("%s: ", st.getRepeatStrategyCmp().getStrategyType())),
                    st.getRepeatStrategy().renderLessParams(historyUpdated)
                );
            }
        } else {
            params = null;
        }
        boolean exerciseCompleted = st.getNextTasks().isEmpty();
        HtmlElem taskContent;
        if (exerciseCompleted) {
            taskContent = frag(
                text("You have completed this exercise. "),
                inpSubmit(ACT_CANCEL_EXERCISE, "Done")
                    .attr("class", "border-on-focus").attr("autofocus", "")
                    .attr("style", format("background-color: %s;", GREEN))
            );
        } else if (st.getTaskState().isPresent()) {
            taskContent = st.getTaskState().get().render();
        } else {
            taskContent = frag(
                text("There are no active tasks. "),
                inpSubmit(ACT_REFRESH_EXERCISE, "Refresh")
                    .attr("class", "border-on-focus").attr("autofocus", "")
                    .attr("style", format("background-color: %s;", GREEN))
            );
        }
        return frag(
            h4(text("Exercise")),
            inpSubmit(ACT_TOGGLE_SHOW_EXERCISE_PARAMS,
                st.getShowMoreParams().isPresent() ? "Hide parameters" : "Show parameters"
            ),
            st.getShowMoreParams().map(showMoreParams ->
                inpSubmit(ACT_TOGGLE_SHOW_LESS_MORE_EXERCISE_PARAMS,
                    showMoreParams ? "Show less parameters" : "Show more parameters"
                )
            ).orElse(null),
            cardPath.isPresent() ? inpSubmit(ACT_OPEN_CARD, "Edit this card") : null,
            st.getTaskState().isPresent() ? inpSubmit(ACT_SKIP_TASK, "Skip this task") : null,
            st.getRepeatStrategy() instanceof RepeatStrategyBuckets
                ? inpSubmit(ACT_TOGGLE_SHOW_DAILY_UNIQUE_COUNT, st.isShowDailyUniqueCount() ? "Hide DUC" : "Show DUC")
                : null,
            text(rndDailyUniqueCount(st)),
            params,
            hr(),
            taskContent,
            hr(),
            exerciseCompleted ? null : inpSubmit(ACT_CANCEL_EXERCISE, "Cancel")
        );
    }

    private String rndDailyUniqueCount(ExerciseState.Started st) {
        if (st.getRepeatStrategy() instanceof RepeatStrategyBuckets && st.isShowDailyUniqueCount()) {
            Optional<Pair<Long, Long>> dailyUniqueCount = st.getRepeatStrategy().getDailyUniqueCount();
            if (dailyUniqueCount.isPresent()) {
                return String.format(" DUC: %s/%s", dailyUniqueCount.get().getLeft(), dailyUniqueCount.get().getRight());
            } else {
                return "";
            }
        } else {
            return "";
        }
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

    private ExerciseState.SetParams makeSetParamsState(
        Settings settings, Cache cache, RequestParams params, ExerciseState prevState
    ) {
        String config = params.getParam(PAR_EXERCISE_CONFIG, cache.getStr(PAR_EXERCISE_CONFIG, ""));
        if (StringUtils.isNotBlank(config)) {
            String finalConfig = config;
            config = settings.getExercises().stream()
                .map(Pair::getLeft)
                .filter(finalConfig::equals)
                .findFirst()
                .orElse("");
        }
        if (StringUtils.isNotBlank(config)) {
            String finalConfig1 = config;
            return settings.getExercises().stream()
                .filter(e -> finalConfig1.equals(e.getLeft()))
                .map(e -> makeSetParamsStateWithPredefinedConfig(settings, cache, e, prevState))
                .findFirst()
                .orElseThrow(() -> new Exn(String.format("Cannot find the exercise with name '%s'", finalConfig1)));
        } else {
            return makeSetParamsStateWithCustomConfig(settings, cache, params, prevState);
        }
    }

    @SneakyThrows
    private ExerciseState.SetParams makeSetParamsStateWithPredefinedConfig(
        Settings settings, Cache cache, Pair<String, File> config, ExerciseState prevState
    ) {
        File configFile = config.getRight();
        Properties props = new Properties();
        try (FileReader fileReader = new FileReader(configFile)) {
            props.load(fileReader);
        }
        String dirStr = props.getProperty(PROP_DIRECTORY);
        if (StringUtils.isBlank(dirStr)) {
            throw new Exn(String.format("%s is not specified in %s.", PROP_DIRECTORY, configFile.getAbsolutePath()));
        }
        dirStr = dirStr.trim();
        File dir = dirStr.startsWith("/") ? new File(dirStr) : new File(configFile.getParentFile(), dirStr);
        DirSelectorCmp dirSelector = new DirSelectorCmpImpl(settings, cache, PAR_DIR_TO_READ_TASKS_FROM, true, dir);
        CardUtils cardUtils = new CardUtilsImpl(utils, settings);
        String tasksStr = props.getProperty(PROP_TASKS);
        tasksStr = StringUtils.isBlank(dirStr) ? "" : tasksStr;
        Set<String> selectedTasks = Arrays.stream(tasksStr.split(","))
            .map(String::trim)
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.toSet());
        return ExerciseState.SetParams.builder()
            .settings(settings)
            .cache(cache)
            .cardUtils(cardUtils)
            .config(config.getLeft())
            .dirSelector(dirSelector)
            .taskTypes(getTaskTypesForPredefinedConfig(
                getAvailableTaskTypes(cardUtils, dirSelector.getSelectedDirectory(), prevState),
                TaskTypeMatcher.fromList(selectedTasks)
            ))
            .repeatStrategyCmp(new RepeatStrategyCmpImpl(
                settings, utils, cache, "PAR_REPEAT_STRATEGY", configFile, props
            ))
            .build();
    }

    private ExerciseState.SetParams makeSetParamsStateWithCustomConfig(
        Settings settings, Cache cache, RequestParams params, ExerciseState prevState
    ) {
        DirSelectorCmp dirSelector = new DirSelectorCmpImpl(settings, cache, PAR_DIR_TO_READ_TASKS_FROM, false, params);
        CardUtils cardUtils = new CardUtilsImpl(utils, settings);
        return ExerciseState.SetParams.builder()
            .settings(settings)
            .cache(cache)
            .cardUtils(cardUtils)
            .config("")
            .dirSelector(dirSelector)
            .taskTypes(getTaskTypes(
                getAvailableTaskTypes(cardUtils, dirSelector.getSelectedDirectory(), prevState), params, cache
            ))
            .repeatStrategyCmp(new RepeatStrategyCmpImpl(settings, utils, cache, "PAR_REPEAT_STRATEGY", params))
            .build();
    }

    private List<TaskType> getAvailableTaskTypes(CardUtils cardUtils, File dir, ExerciseState prevState) {
        File prevDir = switch (prevState) {
            case null -> null;
            case ExerciseState.SetParams st -> st.getDirSelector().getSelectedDirectory();
            case ExerciseState.Started _ -> throw new Exn(
                "getAvailableTaskTypes() should never be called for ExerciseState.Started"
            );
        };
        if (dir.equals(prevDir)) {
            ExerciseState.SetParams st = (ExerciseState.SetParams) prevState;
            return st.getTaskTypes().stream().map(Pair::getLeft).toList();
        }
        return cardUtils.loadAllCards(dir).stream()
            .flatMap(card -> card.getTaskTypes().stream())
            .distinct()
            .sorted(Comparator.comparing(TaskType::getCode))
            .toList();
    }

    private List<Pair<TaskType, Boolean>> getTaskTypes(
        List<TaskType> availableTaskTypes, RequestParams params, Cache cache
    ) {
        Set<String> checked = params.hasParam(keyValueParam(PAR_DIR_TO_READ_TASKS_FROM, 0))
            ? Arrays.stream(params.getParams(PAR_TASK_TYPE)).collect(Collectors.toSet())
            : Arrays.stream(cache.getStr(PAR_TASK_TYPE, "").split(","))
              .filter(StringUtils::isNotBlank)
              .map(String::trim)
              .collect(Collectors.toSet());
        return getTaskTypes(availableTaskTypes, checked);
    }

    private List<Pair<TaskType, Boolean>> getTaskTypes(List<TaskType> availableTaskTypes, Set<String> checked) {
        return availableTaskTypes.stream()
            .map(typ -> Pair.of(typ, checked.contains(typ.getCode())))
            .toList();
    }

    private List<Pair<TaskType, Boolean>> getTaskTypesForPredefinedConfig(
        List<TaskType> availableTaskTypes, TaskTypeMatcher matcher
    ) {
        return availableTaskTypes.stream()
            .map(typ -> Pair.of(typ, matcher.matches(typ)))
            .toList();
    }

    private HtmlElem rndTaskTypes(ExerciseState.SetParams st) {
        return frag(
            h5(text("Task types")),
            table(
                st.getTaskTypes().stream()
                    .map(typ -> {
                        HtmlTag checkbox = inpCheckbox(PAR_TASK_TYPE, typ.getLeft().getCode(), typ.getRight());
                        if (StringUtils.isNotBlank(st.getConfig())) {
                            checkbox.disabled();
                        }
                        return List.of(
                            checkbox,
                            text(typ.getLeft().getCode())
                        );
                    })
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
