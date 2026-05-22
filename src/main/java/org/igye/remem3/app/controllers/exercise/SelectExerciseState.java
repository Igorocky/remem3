package org.igye.remem3.app.controllers.exercise;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.With;
import org.apache.commons.lang3.tuple.Pair;
import org.igye.remem3.app.components.DirSelectorCmp;
import org.igye.remem3.app.controllers.beans.dto.ExerciseDef;
import org.igye.remem3.app.controllers.beans.dto.RepeatStrategyParams;
import org.igye.remem3.app.controllers.beans.dto.TaskFilter;
import org.igye.remem3.app.controllers.beans.dto.TaskView;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

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
    private final Optional<ExerciseDef> selectedExercise;
    @With
    @Getter
    private final DirSelectorCmp selectedDir;
    @With
    @Getter
    private final Optional<Pair<String, TaskFilter>> selectedTaskFilter;
    //numberOfTasks depends on the selected dir and task filter
    @Builder.Default
    private Optional<Pair<Pair<String, String>, Integer>> numberOfTasks = Optional.empty();
    @With
    @Getter
    private final Optional<Pair<String, RepeatStrategyParams>> selectedStrategy;

    public Optional<ExerciseDef> makeSelectedExercise() {
        if (selectedExercise.isPresent()) {
            return selectedExercise;
        }
        if (selectedTaskFilter.isPresent() && selectedStrategy.isPresent()) {
            ExerciseDef.SimpleExerciseDef exerciseDef = new ExerciseDef.SimpleExerciseDef();
            exerciseDef.setName(CUSTOM_EXERCISE_NAME);
            exerciseDef.setDirs(List.of(selectedDir.getSelectedDirectory()));
            exerciseDef.setTaskFilter(selectedTaskFilter.get().getRight());
            exerciseDef.setRepeatStrategy(selectedStrategy.get().getRight());
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
        Pair<String, String> calculatedFor = numberOfTasks.get().getLeft();
        boolean numOfTasksIsStale = !selectedDir.getSelectedDirectoryStr().equals(calculatedFor.getLeft())
            || selectedTaskFilter.map(tf -> !tf.getLeft().equals(calculatedFor.getRight())).orElse(false);
        if (numOfTasksIsStale) {
            numberOfTasks = calcNumberOfTasks();
        }
        return numberOfTasks.map(Pair::getRight);
    }

    private Optional<Pair<Pair<String, String>, Integer>> calcNumberOfTasks() {
        return selectedTaskFilter.map(tf -> {
            long count = taskLoader.apply(selectedDir.getSelectedDirectory()).filter(tf.getRight()::match).count();
            return Pair.of(Pair.of(selectedDir.getSelectedDirectoryStr(), tf.getLeft()), (int) count);
        });
    }

    public SelectExerciseState setSelectedStrategy(String id) {
        return withSelectedStrategy(
            allStrategies.stream()
                .filter(pair -> pair.getLeft().equals(id))
                .findFirst()
                .or(() -> Optional.ofNullable(allStrategies.isEmpty() ? null : allStrategies.getFirst()))
        );
    }


}
