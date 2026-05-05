package org.igye.remem3.app.controllers2.exercise;

import lombok.Setter;
import org.igye.remem3.app.state.StateConstructor;
import org.igye.remem3.app.state.StateLookup;

public class ExerciseConstructor implements StateConstructor<ExerciseState> {
    @Setter
    private StateLookup stateLookup;

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
