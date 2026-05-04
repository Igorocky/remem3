package org.igye.remem3.app.controllers2.exercise;

import org.igye.remem3.app.dto.Task;
import org.igye.remem3.app.repeatstrategy.RepeatStrategy;
import org.igye.remem3.app.taskstate.TaskState;

import java.util.List;
import java.util.Optional;

public final class RunningExerciseState implements ExerciseState {
    private List<String> errors;
    private Optional<Boolean> showMoreParams;
    private boolean cardPathCopied;
    private boolean showDailyUniqueCount;

    private RepeatStrategy repeatStrategy;
    private Optional<List<Task>> nextTasks;
    private Optional<TaskState> taskState;
}
