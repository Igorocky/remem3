package org.igye.remem3.app.controllers.exercise;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.With;
import org.apache.commons.lang3.tuple.Pair;
import org.igye.remem3.app.controllers.beans.dto.ExerciseDef;
import org.igye.remem3.app.controllers.beans.dto.RepeatStrategyParams;
import org.igye.remem3.app.controllers.beans.dto.TaskFilter;
import org.igye.remem3.app.controllers.components.DirSelectorCmp;

import java.util.List;
import java.util.Optional;

import static org.igye.remem3.app.controllers.exercise.ExerciseRenderer.CUSTOM_EXERCISE_NAME;

@AllArgsConstructor
@Builder
@Getter
public final class SelectExerciseState implements ExerciseState {
    private final List<ExerciseDef> allExercises;
    private final List<Pair<String, TaskFilter>> allTaskFilters;
    private final List<Pair<String, RepeatStrategyParams>> allStrategies;

    @With
    private final Optional<ExerciseDef> selectedExercise;
    @With
    private final DirSelectorCmp selectedDir;
    @With
    private final Optional<Pair<String, TaskFilter>> selectedTaskFilter;
    @With
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

    public SelectExerciseState setSelectedStrategy(String id) {
        return withSelectedStrategy(
            allStrategies.stream()
                .filter(pair -> pair.getLeft().equals(id))
                .findFirst()
                .or(() -> Optional.ofNullable(allStrategies.isEmpty() ? null : allStrategies.getFirst()))
        );
    }


}
