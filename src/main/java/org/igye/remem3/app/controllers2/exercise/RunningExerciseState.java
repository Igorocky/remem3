package org.igye.remem3.app.controllers2.exercise;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.With;
import org.igye.remem3.app.controllers.exercise.HasBaseTask;
import org.igye.remem3.app.dto.Card;
import org.igye.remem3.app.repeatstrategy.RepeatStrategy;
import org.igye.remem3.app.repeatstrategy.Task;
import org.igye.remem3.app.taskstate.TaskState;
import org.igye.remem3.utils.Exn;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@RequiredArgsConstructor
@AllArgsConstructor
public final class RunningExerciseState implements ExerciseState {
    @Getter
    private final ExerciseState parent;
    private List<String> errors = List.of();
    @Getter
    @With
    private Optional<Boolean> showMoreParams = Optional.empty();
    @Getter
    @Setter
    @With
    private boolean cardPathCopied = false;
    @Getter
    @With
    private boolean showDailyUniqueCount = false;

    @Getter
    private final Set<String> directories;
    @Getter
    private final Set<String> taskTypes;
    @Getter
    private final Set<String> repeatStrategyTypes;

    @Getter
    private final RepeatStrategy repeatStrategy;
    @Getter
    @With
    private Optional<List<Task>> nextTasks;
    @Getter
    @With
    private Optional<TaskState> taskState;

    public Optional<Card> getCurrentCard() {
        return nextTasks
            .flatMap(nextTasks -> nextTasks.isEmpty() ? Optional.empty() : Optional.of(nextTasks.getFirst()))
            .map(this::getBaseTask)
            .map(org.igye.remem3.app.dto.Task::getCard);
    }

    public Card getCurrentCardExn() {
        return getCurrentCard().orElseThrow(() -> new Exn("Cannot get the card for the current task."));
    }

    public Optional<File> getCurrentCardFile() {
        return getCurrentCard().flatMap(Card::getFile);
    }

    public File getCurrentCardFileExn() {
        return getCurrentCardFile().orElseThrow(() -> new Exn("Cannot determine the file for the current task."));
    }

    public boolean isExerciseCompleted() {
        return nextTasks.isEmpty();
    }

    private org.igye.remem3.app.dto.Task getBaseTask(Task task) {
        return ((HasBaseTask) task).getBaseTask();
    }
}
