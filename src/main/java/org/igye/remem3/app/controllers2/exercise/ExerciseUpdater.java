package org.igye.remem3.app.controllers2.exercise;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.igye.remem3.app.Cache;
import org.igye.remem3.app.CardUtils;
import org.igye.remem3.app.Settings;
import org.igye.remem3.app.controllers.exercise.HasBaseTask;
import org.igye.remem3.app.dto.Card;
import org.igye.remem3.app.dto.RepeatStrategyType;
import org.igye.remem3.app.dto.Task;
import org.igye.remem3.app.dto.TaskType;
import org.igye.remem3.app.state.StateUpdater;
import org.igye.remem3.app.taskstate.TaskResult;
import org.igye.remem3.app.taskstate.TaskState;
import org.igye.remem3.app.taskstate.impl.TaskStateFillGaps;
import org.igye.remem3.app.taskstate.impl.TaskStateTranslate;
import org.igye.remem3.utils.NotImplemented;
import org.igye.remem3.utils.Utils;
import org.igye.remem3.web.RequestParams;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.igye.remem3.app.controllers2.exercise.ExerciseRenderer.ACT_CANCEL_EXERCISE;
import static org.igye.remem3.app.controllers2.exercise.ExerciseRenderer.ACT_COPY_CARD_PATH_TO_CLIPBOARD;
import static org.igye.remem3.app.controllers2.exercise.ExerciseRenderer.ACT_OPEN_CARD;
import static org.igye.remem3.app.controllers2.exercise.ExerciseRenderer.ACT_REFRESH_EXERCISE;
import static org.igye.remem3.app.controllers2.exercise.ExerciseRenderer.ACT_SKIP_TASK;
import static org.igye.remem3.app.controllers2.exercise.ExerciseRenderer.ACT_TOGGLE_SHOW_DAILY_UNIQUE_COUNT;
import static org.igye.remem3.app.controllers2.exercise.ExerciseRenderer.ACT_TOGGLE_SHOW_EXERCISE_PARAMS;
import static org.igye.remem3.app.controllers2.exercise.ExerciseRenderer.ACT_TOGGLE_SHOW_LESS_MORE_EXERCISE_PARAMS;
import static org.igye.remem3.app.controllers2.exercise.ExerciseRenderer.PAR_SHOW_DAILY_UNIQUE_COUNT;
import static org.igye.remem3.app.controllers2.exercise.ExerciseRenderer.PAR_SHOW_EXERCISE_PARAMS;

@RequiredArgsConstructor
public class ExerciseUpdater implements StateUpdater<ExerciseState> {
    private final Cache cache;
    private final CardUtils cardUtils;
    private final Clock clock;
    private final Utils utils;
    private final Settings settings;

    @Override
    public ExerciseState update(ExerciseState st, RequestParams params) {
        return switch (st) {
            case SelectExerciseState sel -> updateSelectExerciseState(sel, params);
            case RunningExerciseState run -> updateRunningExerciseState(run, params);
        };
    }

    private ExerciseState updateSelectExerciseState(SelectExerciseState st, RequestParams params) {
        throw new NotImplemented();
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
}
