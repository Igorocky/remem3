package org.igye.remem3.app.controllers2.exercise;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.igye.remem3.app.dto.Card;
import org.igye.remem3.app.dto.Task;
import org.igye.remem3.app.repeatstrategy.RepeatStrategy;
import org.igye.remem3.app.taskstate.TaskState;
import org.igye.remem3.utils.Exn;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@RequiredArgsConstructor
public final class RunningExerciseState implements ExerciseState {
    private List<String> errors = List.of();
    @Getter
    private Optional<Boolean> showMoreParams = Optional.empty();
    @Getter
    @Setter
    private boolean cardPathCopied = false;
    @Getter
    private boolean showDailyUniqueCount = false;

    @Getter
    private final Set<String> directories;
    @Getter
    private final Set<String> taskTypes;
    @Getter
    private final Set<String> repeatStrategyTypes;

    @Getter
    private final RepeatStrategy repeatStrategy;
    private Optional<List<Task>> nextTasks;
    @Getter
    private Optional<TaskState> taskState;

    public Optional<Card> getCurrentCard() {
        return nextTasks
            .flatMap(nextTasks -> nextTasks.isEmpty() ? Optional.empty() : Optional.of(nextTasks.getFirst()))
            .map(Task::getCard);
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
}
