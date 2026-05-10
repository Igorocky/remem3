package org.igye.remem3.app.controllers2.exercise;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.tuple.Pair;
import org.igye.remem3.app.Cache;
import org.igye.remem3.app.Settings;
import org.igye.remem3.app.controllers.components.impl.DirSelectorCmpImpl;
import org.igye.remem3.app.controllers2.beans.BeansState;
import org.igye.remem3.app.controllers2.beans.dto.ExerciseDef;
import org.igye.remem3.app.controllers2.beans.dto.RepeatStrategyParams;
import org.igye.remem3.app.controllers2.beans.dto.TaskFilter;
import org.igye.remem3.app.state.StateConstructor;
import org.igye.remem3.app.state.StateLookup;
import org.springframework.core.annotation.Order;

import java.io.File;
import java.util.Comparator;
import java.util.List;

import static org.igye.remem3.app.controllers2.beans.BeansConstructor.BEANS;
import static org.igye.remem3.app.controllers2.exercise.ExerciseRenderer.PAR_SELECTED_DIR;
import static org.igye.remem3.app.controllers2.exercise.ExerciseRenderer.PAR_SELECTED_EXERCISE;
import static org.igye.remem3.app.controllers2.exercise.ExerciseRenderer.PAR_SELECTED_STRATEGY;
import static org.igye.remem3.app.controllers2.exercise.ExerciseRenderer.PAR_SELECTED_TASK_FILTER;

@RequiredArgsConstructor
@Order(2)
public class ExerciseConstructor implements StateConstructor<ExerciseState> {
    private final Cache cache;
    private final Settings settings;
    @Setter
    private StateLookup stateLookup;

    @Override
    public String getName() {
        return "exercise";
    }

    @Override
    public String getDisplayName() {
        return "Exercises";
    }

    @Override
    public ExerciseState construct() {
        BeansState beans = stateLookup.getState(BEANS);
        List<ExerciseDef> allExercises = beans.getBeans(ExerciseDef.class).stream()
            .map(pair -> {
                switch (pair.getRight()) {
                    case ExerciseDef.BaseExerciseDef ex -> ex.setName(pair.getLeft());
                }
                return pair.getRight();
            })
            .sorted(Comparator.comparing(ExerciseDef::getName))
            .toList();
        List<Pair<String, TaskFilter>> allTaskFilters = beans.getBeans(TaskFilter.class).stream()
            .sorted(Comparator.comparing(Pair::getLeft))
            .toList();
        List<Pair<String, RepeatStrategyParams>> allStrategies = beans.getBeans(RepeatStrategyParams.class).stream()
            .sorted(Comparator.comparing(Pair::getLeft))
            .toList();

        SelectExerciseState st = SelectExerciseState.builder()
            .allExercises(allExercises)
            .allTaskFilters(allTaskFilters)
            .allStrategies(allStrategies)
            .build();
        st = st.setSelectedExercise(cache.getStr(PAR_SELECTED_EXERCISE, ""));
        st = st.withSelectedDir(new DirSelectorCmpImpl(settings, cache, PAR_SELECTED_DIR, false,
            new File(cache.getStr(PAR_SELECTED_DIR, ""))
        ));
        st = st.setSelectedTaskFilter(cache.getStr(PAR_SELECTED_TASK_FILTER, ""));
        st = st.setSelectedStrategy(cache.getStr(PAR_SELECTED_STRATEGY, ""));
        return st;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
