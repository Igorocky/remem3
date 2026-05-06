package org.igye.remem3.app.controllers2.exercise;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.igye.remem3.app.Cache;
import org.igye.remem3.app.controllers2.beans.BeansState;
import org.igye.remem3.app.controllers2.beans.dto.ExerciseDef;
import org.igye.remem3.app.state.StateConstructor;
import org.igye.remem3.app.state.StateLookup;

import java.util.Comparator;
import java.util.List;

import static org.igye.remem3.app.controllers2.beans.BeansConstructor.BEANS;
import static org.igye.remem3.app.controllers2.exercise.ExerciseRenderer.PAR_EXERCISE_DEF;

@RequiredArgsConstructor
public class ExerciseConstructor implements StateConstructor<ExerciseState> {
    private final Cache cache;
    @Setter
    private StateLookup stateLookup;

    @Override
    public String getName() {
        return "exercise";
    }

    @Override
    public ExerciseState construct() {
        BeansState beans = stateLookup.getState(BEANS);
        List<ExerciseDef> exercises = beans.getCtx().getBeansOfType(ExerciseDef.class).values().stream()
            .sorted(Comparator.comparing(ExerciseDef::getName))
            .toList();
        String selectedExName = cache.getStr(
            PAR_EXERCISE_DEF, exercises.isEmpty() ? "" : exercises.getFirst().getName()
        );
        ExerciseDef selectedEx = exercises.stream()
            .filter(ex -> selectedExName.equals(ex.getName()))
            .findFirst()
            .orElse(exercises.isEmpty() ? null : exercises.getFirst());

        return SelectExerciseState.builder()
            .allExercises(exercises)
            .selectedExercise(selectedEx)
            .build();
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
