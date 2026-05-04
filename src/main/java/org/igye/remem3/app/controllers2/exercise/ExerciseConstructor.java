package org.igye.remem3.app.controllers2.exercise;

import org.igye.remem3.app.state.StateConstructor;

public class ExerciseConstructor implements StateConstructor<ExerciseState> {
    @Override
    public String getName() {
        return "exercise";
    }

    @Override
    public ExerciseState construct() {
        return null;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
