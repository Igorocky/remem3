package org.igye.remem3.app.controllers.exercise;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.igye.remem3.app.Cache;
import org.igye.remem3.app.CardUtils;
import org.igye.remem3.app.Settings;
import org.igye.remem3.app.components.impl.DirSelectorCmpImpl;
import org.igye.remem3.app.controllers.beans.dto.ExerciseDef;
import org.igye.remem3.app.controllers.beans.dto.RepeatStrategyParams;
import org.igye.remem3.app.controllers.beans.dto.TaskViewImpl;
import org.igye.remem3.app.dto.Card;
import org.igye.remem3.app.dto.RepeatStrategyType;
import org.igye.remem3.app.dto.Task;
import org.igye.remem3.app.dto.TaskType;
import org.igye.remem3.app.repeatstrategy.RepeatStrategy;
import org.igye.remem3.app.repeatstrategy.impl.RepeatStrategyBuckets;
import org.igye.remem3.app.repeatstrategy.impl.RepeatStrategyCircle;
import org.igye.remem3.app.repeatstrategy.impl.RepeatStrategyCompound;
import org.igye.remem3.app.repeatstrategy.impl.RepeatStrategyQueue;
import org.igye.remem3.app.repeatstrategy.impl.RepeatStrategyRetryFailed;
import org.igye.remem3.app.repeatstrategy.impl.TaskImpl;
import org.igye.remem3.app.state.StateUpdater;
import org.igye.remem3.app.taskstate.TaskResult;
import org.igye.remem3.app.taskstate.TaskState;
import org.igye.remem3.app.taskstate.impl.TaskStateFillGaps;
import org.igye.remem3.app.taskstate.impl.TaskStateTranslate;
import org.igye.remem3.utils.Exn;
import org.igye.remem3.utils.NatOrdPath;
import org.igye.remem3.utils.Utils;
import org.igye.remem3.utils.impl.NatOrdPathImpl;
import org.igye.remem3.web.RequestParams;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static org.igye.remem3.app.controllers.exercise.ExerciseRenderer.ACT_CANCEL_EXERCISE;
import static org.igye.remem3.app.controllers.exercise.ExerciseRenderer.ACT_COPY_CARD_PATH_TO_CLIPBOARD;
import static org.igye.remem3.app.controllers.exercise.ExerciseRenderer.ACT_OPEN_CARD;
import static org.igye.remem3.app.controllers.exercise.ExerciseRenderer.ACT_REFRESH_EXERCISE;
import static org.igye.remem3.app.controllers.exercise.ExerciseRenderer.ACT_SKIP_TASK;
import static org.igye.remem3.app.controllers.exercise.ExerciseRenderer.ACT_START_EXERCISE;
import static org.igye.remem3.app.controllers.exercise.ExerciseRenderer.ACT_TOGGLE_SHOW_DAILY_UNIQUE_COUNT;
import static org.igye.remem3.app.controllers.exercise.ExerciseRenderer.ACT_TOGGLE_SHOW_EXERCISE_PARAMS;
import static org.igye.remem3.app.controllers.exercise.ExerciseRenderer.ACT_TOGGLE_SHOW_LESS_MORE_EXERCISE_PARAMS;
import static org.igye.remem3.app.controllers.exercise.ExerciseRenderer.PAR_SELECTED_DIR;
import static org.igye.remem3.app.controllers.exercise.ExerciseRenderer.PAR_SELECTED_EXERCISE;
import static org.igye.remem3.app.controllers.exercise.ExerciseRenderer.PAR_SELECTED_STRATEGY;
import static org.igye.remem3.app.controllers.exercise.ExerciseRenderer.PAR_SELECTED_TASK_FILTER;
import static org.igye.remem3.app.controllers.exercise.ExerciseRenderer.PAR_SHOW_DAILY_UNIQUE_COUNT;
import static org.igye.remem3.app.controllers.exercise.ExerciseRenderer.PAR_SHOW_EXERCISE_PARAMS;

@RequiredArgsConstructor
public class ExerciseUpdater implements StateUpdater<ExerciseState> {
    private final Cache cache;
    private final CardUtils cardUtils;
    private final Clock clock;
    private final Utils utils;
    private final Settings settings;
    private final ExerciseConstructor constructor;

    @Override
    public ExerciseState update(ExerciseState st, RequestParams params) {
        return switch (st) {
            case SelectExerciseState sel -> updateSelectExerciseState(sel, params);
            case RunningExerciseState run -> updateRunningExerciseState(run, params);
        };
    }

    private SelectExerciseState updateSelectablePart(
        SelectExerciseState st,
        RequestParams params,
        String paramName,
        Function<String, SelectExerciseState> setter
    ) {
        if (params.hasParam(paramName)) {
            String id = params.getParam(paramName);
            cache.put(paramName, id);
            return setter.apply(id);
        }
        return st;
    }

    private ExerciseState updateSelectExerciseState(SelectExerciseState st, RequestParams params) {
        st = updateSelectablePart(st, params, PAR_SELECTED_EXERCISE, st::setSelectedExercise);
        if (params.hasKeyValueParam(PAR_SELECTED_DIR)) {
            st = st.withSelectedDir(new DirSelectorCmpImpl(settings, cache, PAR_SELECTED_DIR).setPath(params));
            cache.put(PAR_SELECTED_DIR, st.getSelectedDir().getSelectedDirectoryStr());
        }
        SelectExerciseState finalSt = st;
        st = updateSelectablePart(st, params, PAR_SELECTED_TASK_FILTER, finalSt::setSelectedTaskFilter);
        st = updateSelectablePart(st, params, PAR_SELECTED_STRATEGY, st::setSelectedStrategy);
        if (params.hasParam(ACT_START_EXERCISE)) {
            return actStartExercise(st);
        }
        return st;
    }

    private ExerciseState actStartExercise(SelectExerciseState st) {
        Optional<ExerciseDef> exerciseToStart = st.makeSelectedExercise();
        if (exerciseToStart.isEmpty()) {
            return st;
        }
        SelectExerciseState parent = st;
        ExerciseDef selectedExercise = exerciseToStart.get();
        Pair<List<org.igye.remem3.app.repeatstrategy.Task>, RepeatStrategy> pair = makeRepeatStrategy(selectedExercise);
        RepeatStrategy repeatStrategy = pair.getRight();
        RunningExerciseState runningSt = new RunningExerciseState(
            parent, selectedExercise.getName(), repeatStrategy
        )
            .withShowMoreParams(parseShowMoreParams(cache.getStr(PAR_SHOW_EXERCISE_PARAMS, "false")))
            .withShowDailyUniqueCount(cache.getBool(PAR_SHOW_DAILY_UNIQUE_COUNT, false));
        return actGoToNextTask(runningSt);
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

    private Pair<List<org.igye.remem3.app.repeatstrategy.Task>, RepeatStrategy> makeRepeatStrategy(ExerciseDef ex) {
        return switch (ex) {
            case ExerciseDef.SimpleExerciseDef s -> makeSimpleRepeatStrategy(s);
            case ExerciseDef.CompoundExerciseDef c -> makeCompoundRepeatStrategy(c);
        };
    }

    private Pair<List<org.igye.remem3.app.repeatstrategy.Task>, RepeatStrategy> makeCompoundRepeatStrategy(
        ExerciseDef.CompoundExerciseDef compEx
    ) {
        validateDirs(compEx.getDirectories());
        List<Pair<Integer, Pair<List<org.igye.remem3.app.repeatstrategy.Task>, RepeatStrategy>>> tasksAndStrategies =
            compEx.getExercises().stream()
                .map(pair -> Pair.of(pair.getLeft(), makeRepeatStrategy(pair.getRight())))
                .toList();
        List<org.igye.remem3.app.repeatstrategy.Task> allTasks = tasksAndStrategies.stream()
            .map(p -> p.getRight().getLeft())
            .flatMap(Collection::stream)
            .toList();
        RepeatStrategyCompound strategy = new RepeatStrategyCompound(
            tasksAndStrategies.stream()
                .map(p -> Pair.of(p.getLeft(), p.getRight().getRight()))
                .toList()
        );
        return Pair.of(allTasks, strategy);
    }

    private Pair<List<org.igye.remem3.app.repeatstrategy.Task>, RepeatStrategy> makeSimpleRepeatStrategy(
        ExerciseDef.SimpleExerciseDef simpEx
    ) {
        validateDirs(simpEx.getDirectories());
        RepeatStrategyType repeatStrategyType = simpEx.getRepeatStrategy().getRepeatStrategyType();
        Instant historyStartsAt = simpEx.getRepeatStrategy().getStartTime().get();
        Comparator<Pair<NatOrdPath, Card>> comparator = Comparator.comparing((Pair<NatOrdPath, Card> pair) ->
            pair.getLeft()
        ).thenComparing((Pair<NatOrdPath, Card> pair) ->
            pair.getRight().getOrder()
        );
        List<org.igye.remem3.app.repeatstrategy.Task> allTasks = simpEx.getDirectories().stream()
            .map(File::new)
            .map(cardUtils::loadAllCards)
            .flatMap(Collection::stream)
            .map(card -> Pair.of(((NatOrdPath) new NatOrdPathImpl(card.getFile().get().getParentFile())), card))
            .sorted(comparator)
            .map(Pair::getRight)
            .map(Card::getTasks)
            .flatMap(Collection::stream)
            .map(constructor::makeTaskView)
            .filter(simpEx.getTaskFilter()::match)
            .map(TaskViewImpl.class::cast)
            .map(TaskViewImpl::getTask)
            .map(task -> makeTaskForRepeatStrategy(task, historyStartsAt, repeatStrategyType))
            .toList();
        RepeatStrategy repeatStrategy = switch (simpEx.getRepeatStrategy()) {
            case RepeatStrategyParams.RepeatStrategyCircleParams p -> new RepeatStrategyCircle(
                utils, allTasks, p.getRandomness(), p.getRounds(), p.isKeepOrder()
            );
            case RepeatStrategyParams.RepeatStrategyRetryFailedParams p -> new RepeatStrategyRetryFailed(
                utils, allTasks, p.getRandomness(), p.isKeepOrder()
            );
            case RepeatStrategyParams.RepeatStrategyQueueParams p -> new RepeatStrategyQueue(
                utils, allTasks, p.getStep(), p.getStepMultFactor(), p.getBatchSize()
            );
            case RepeatStrategyParams.RepeatStrategyBucketsParams p -> new RepeatStrategyBuckets(
                utils, clock, p.getBatchSize(), allTasks, p.getDelays()
            );
        };
        return Pair.of(allTasks, repeatStrategy);
    }

    private org.igye.remem3.app.repeatstrategy.Task makeTaskForRepeatStrategy(
        Task task,
        Instant historyStartsAt,
        RepeatStrategyType repeatStrategyType
    ) {
        return new TaskImpl(task, historyStartsAt, repeatStrategyType);
    }

    private ExerciseState updateRunningExerciseState(RunningExerciseState st, RequestParams params) {
        if (params.hasParam(ACT_CANCEL_EXERCISE)) {
            return actCancelExercise(st);
        }
        if (params.hasParam(ACT_TOGGLE_SHOW_EXERCISE_PARAMS)) {
            return actToggleShowParams(st);
        }
        if (params.hasParam(ACT_TOGGLE_SHOW_LESS_MORE_EXERCISE_PARAMS)) {
            return actToggleShowLessMoreParams(st);
        }
        if (params.hasParam(ACT_TOGGLE_SHOW_DAILY_UNIQUE_COUNT)) {
            return actToggleShowDailyUniqueCount(st);
        }
        if (params.hasParam(ACT_SKIP_TASK) || params.hasParam(ACT_REFRESH_EXERCISE)) {
            return actGoToNextTask(st);
        }
        if (params.hasParam(ACT_COPY_CARD_PATH_TO_CLIPBOARD)) {
            return actCopyCardPathToClipboard(st);
        }
        if (params.hasParam(ACT_OPEN_CARD)) {
            return actOpenCard(st);
        }
        if (st.getTaskState().isPresent()) {
            return actProcessTaskResults(st, st.getTaskState().get().processUserInput(params));
        }
        return st;
    }

    private RunningExerciseState actToggleShowParams(RunningExerciseState st) {
        RunningExerciseState newState;
        if (st.getShowMoreParams().isEmpty()) {
            newState = st.withShowMoreParams(Optional.of(true));
        } else {
            newState = st.withShowMoreParams(Optional.empty());
        }
        cacheShowMoreParams(newState);
        return newState;
    }

    private void cacheShowMoreParams(RunningExerciseState st) {
        cache.put(PAR_SHOW_EXERCISE_PARAMS, st.getShowMoreParams().map(Object::toString).orElse(""));
    }

    private static ExerciseState actCancelExercise(RunningExerciseState st) {
        return st.getParent();
    }

    private RunningExerciseState actToggleShowLessMoreParams(RunningExerciseState st) {
        RunningExerciseState newState;
        if (st.getShowMoreParams().isEmpty()) {
            newState = st.withShowMoreParams(Optional.of(true));
        } else {
            newState = st.withShowMoreParams(Optional.of(!st.getShowMoreParams().get()));
        }
        cacheShowMoreParams(newState);
        return newState;
    }

    private RunningExerciseState actToggleShowDailyUniqueCount(RunningExerciseState st) {
        RunningExerciseState newState = st.withShowDailyUniqueCount(!st.isShowDailyUniqueCount());
        cache.put(PAR_SHOW_DAILY_UNIQUE_COUNT, newState.isShowDailyUniqueCount());
        return newState;
    }

    private RunningExerciseState actGoToNextTask(RunningExerciseState st) {
        st.getCurrentCard().ifPresent(card ->
            card.copyFrom(cardUtils.loadCard(st.getCurrentCardFileExn()))
        );
        Optional<List<org.igye.remem3.app.repeatstrategy.Task>> nextTasksOpt = st.getNextTasks().flatMap(tasks -> {
            if (tasks.size() < 2) {
                return st.getRepeatStrategy().getNextTasks();
            } else {
                List<org.igye.remem3.app.repeatstrategy.Task> tail = new ArrayList<>(tasks);
                tail.removeFirst();
                return Optional.of(tail);
            }
        });
        if (nextTasksOpt.isEmpty() || nextTasksOpt.get().isEmpty()) {
            return st
                .withNextTasks(nextTasksOpt)
                .withTaskState(Optional.empty());
        }
        org.igye.remem3.app.repeatstrategy.Task nextTask = nextTasksOpt.get().getFirst();
        return st
            .withNextTasks(nextTasksOpt)
            .withTaskState(Optional.of(makeTaskState(nextTask)));
    }

    private TaskState makeTaskState(org.igye.remem3.app.repeatstrategy.Task task) {
        Task baseTask = getBaseTask(task);
        final Card card = baseTask.getCard();
        final RepeatStrategyType selectedByStrategyType = task.getSelectedByStrategyType();
        return switch (baseTask.getTaskType()) {
            case TaskType.FillGaps t ->
                new TaskStateFillGaps(clock, utils, cardUtils, (Card.FillGaps) card, t, selectedByStrategyType);
            case TaskType.Translate t ->
                new TaskStateTranslate(clock, utils, cardUtils, (Card.Translate) card, t, selectedByStrategyType);
        };
    }

    private RunningExerciseState actCopyCardPathToClipboard(RunningExerciseState st) {
        StringSelection stringSelection = new StringSelection(st.getCurrentCardFileExn().getAbsolutePath());
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, null);
        return st.withCardPathCopied(true);
    }

    @SneakyThrows
    private RunningExerciseState actOpenCard(RunningExerciseState st) {
        new ProcessBuilder(
            settings.getCardEditor(),
            st.getCurrentCardFileExn().getAbsolutePath()
        ).start();
        return st;
    }

    @SneakyThrows
    private RunningExerciseState actProcessTaskResults(RunningExerciseState st, TaskResult taskResult) {
        if (taskResult.getHistRec().isPresent()) {
            File file = st.getCurrentCardFileExn();
            cardUtils.appendHistRecToFile(file, taskResult.getHistRec().get());
            st.getCurrentCardExn().copyFrom(cardUtils.loadCard(file));
        }
        if (taskResult.isCompleted()) {
            return actGoToNextTask(st);
        }
        return st;
    }

    private Task getBaseTask(org.igye.remem3.app.repeatstrategy.Task task) {
        return ((HasBaseTask) task).getBaseTask();
    }

    private void validateDirs(List<String> dirs) {
        List<String> paths = dirs.stream()
            .map(File::new)
            .map(this::getCanonicalPath)
            .distinct()
            .toList();
        for (int i = 0; i < paths.size(); i++) {
            for (int j = 0; j < paths.size(); j++) {
                if (i != j) {
                    String p1 = paths.get(i);
                    String p2 = paths.get(j);
                    if (p1.startsWith(p2)) {
                        throw new Exn("Directories cannot be nested, but got '%s' is a subdirectory of '%s'.".formatted(
                            p2, p1
                        ));
                    }
                }
            }
        }
    }

    @SneakyThrows
    private String getCanonicalPath(File file) {
        return file.getCanonicalPath();
    }
}
