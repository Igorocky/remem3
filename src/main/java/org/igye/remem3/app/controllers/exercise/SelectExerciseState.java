package org.igye.remem3.app.controllers.exercise;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.With;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.igye.remem3.app.components.DirSelectorCmp;
import org.igye.remem3.app.components.PrioritySelectorCmp;
import org.igye.remem3.app.controllers.beans.dto.ExerciseDef;
import org.igye.remem3.app.controllers.beans.dto.RepeatStrategyParams;
import org.igye.remem3.app.controllers.beans.dto.TaskFilter;
import org.igye.remem3.app.controllers.beans.dto.TaskView;

import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.igye.remem3.app.components.impl.PrioritySelectorCmpImpl.NUM_OF_PRIORITIES;
import static org.igye.remem3.app.controllers.exercise.ExerciseRenderer.CUSTOM_EXERCISE_NAME;

@AllArgsConstructor
@Builder
public final class SelectExerciseState implements ExerciseState {
    @Getter
    private final List<ExerciseDef> allExercises;
    @Getter
    private final List<Pair<String, TaskFilter>> allTaskFilters;
    @Getter
    private final List<Pair<String, RepeatStrategyParams>> allStrategies;
    private final Function<File, Stream<TaskView>> taskLoader;

    @With
    @Getter
    @Builder.Default
    private List<String> errors = new ArrayList<>();
    @With
    @Getter
    private final Optional<ExerciseDef> selectedExercise;
    @With
    @Getter
    private final DirSelectorCmp selectedDir;
    @With
    @Getter
    private final PrioritySelectorCmp prioritySelector;
    @With
    @Getter
    private final Optional<Pair<String, TaskFilter>> selectedTaskFilter;
    //numberOfTasks depends on the selected dir and task filter
    @Builder.Default
    private Optional<Pair<List<String>, Integer>> numberOfTasks = Optional.empty();
    @With
    @Getter
    private final Optional<Pair<String, RepeatStrategyParams>> selectedStrategy;
    @With
    @Getter
    private final Optional<String> startTime;

    public Optional<ExerciseDef> makeSelectedExercise(boolean parseStartTime) {
        if (selectedExercise.isPresent()) {
            return selectedExercise;
        }
        if (selectedTaskFilter.isPresent() && selectedStrategy.isPresent()) {
            ExerciseDef.SimpleExerciseDef exerciseDef = new ExerciseDef.SimpleExerciseDef();
            exerciseDef.setName(CUSTOM_EXERCISE_NAME);
            exerciseDef.setDirs(List.of(selectedDir.getSelectedDirectory()));
            Set<Integer> selectedPriorities = prioritySelector.getSelectedPriorities();
            boolean selectAllPriorities = selectedPriorities.isEmpty();
            exerciseDef.setTaskFilter(
                selectedTaskFilter.get().getRight()
                    .and(task -> selectAllPriorities || selectedPriorities.contains(task.getPriority()))
            );
            exerciseDef.setRepeatStrategy(selectedStrategy.get().getRight());
            if (parseStartTime && startTime.isPresent()) {
                exerciseDef.setStartTime(Instant.parse(this.startTime.get()));
            }
            return Optional.of(exerciseDef);
        }
        return Optional.empty();
    }

    public SelectExerciseState setSelectedExercise(String id) {
        if (CUSTOM_EXERCISE_NAME.equals(id)) {
            return withSelectedExercise(Optional.empty());
        }
        return withSelectedExercise(
            allExercises.stream()
                .filter(ex -> ex.getName().equals(id))
                .findFirst()
                .or(() -> Optional.ofNullable(allExercises.isEmpty() ? null : allExercises.getFirst()))
        );
    }

    public SelectExerciseState setSelectedTaskFilter(String id) {
        return withSelectedTaskFilter(
            allTaskFilters.stream()
                .filter(pair -> pair.getLeft().equals(id))
                .findFirst()
                .or(() -> Optional.ofNullable(allTaskFilters.isEmpty() ? null : allTaskFilters.getFirst()))
        );
    }

    public Optional<Integer> getNumberOfTasks() {
        if (selectedExercise.isPresent()) {
            return Optional.empty();
        }
        if (numberOfTasks.isEmpty()) {
            numberOfTasks = calcNumberOfTasks();
            return numberOfTasks.map(Pair::getRight);
        }
        List<String> calculatedFor = numberOfTasks.get().getLeft();
        boolean numOfTasksIsStale = !selectedDir.getSelectedDirectoryStr().equals(calculatedFor.get(0))
            || !getSelectedPrioritiesStr(prioritySelector.getSelectedPriorities()).equals(calculatedFor.get(1))
            || selectedTaskFilter.map(tf -> !tf.getLeft().equals(calculatedFor.get(2))).orElse(false);
        if (numOfTasksIsStale) {
            numberOfTasks = calcNumberOfTasks();
        }
        return numberOfTasks.map(Pair::getRight);
    }

    private Optional<Pair<List<String>, Integer>> calcNumberOfTasks() {
        return selectedTaskFilter.map(tf -> {
            Set<Integer> selectedPriorities = prioritySelector.getSelectedPriorities();
            boolean selectAllPriorities = selectedPriorities.isEmpty();
            long count = taskLoader.apply(selectedDir.getSelectedDirectory())
                .filter(task -> selectAllPriorities || selectedPriorities.contains(task.getPriority()))
                .filter(tf.getRight()::match)
                .count();
            return Pair.of(
                List.of(
                    selectedDir.getSelectedDirectoryStr(),
                    getSelectedPrioritiesStr(selectedPriorities),
                    tf.getLeft()
                ),
                (int) count
            );
        });
    }

    private String getSelectedPrioritiesStr(Set<Integer> selectedPriorities) {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= NUM_OF_PRIORITIES; i++) {
            sb.append(selectedPriorities.contains(i) ? 1 : 0);
        }
        return sb.toString();
    }

    public SelectExerciseState setSelectedStrategy(String id) {
        return withSelectedStrategy(
            allStrategies.stream()
                .filter(pair -> pair.getLeft().equals(id))
                .findFirst()
                .or(() -> Optional.ofNullable(allStrategies.isEmpty() ? null : allStrategies.getFirst()))
        );
    }

    public SelectExerciseState setStartTime(String startTime) {
        if (StringUtils.isBlank(startTime)) {
            return withStartTime(Optional.empty());
        }
        return withStartTime(Optional.of(startTime.trim()));
    }
}
