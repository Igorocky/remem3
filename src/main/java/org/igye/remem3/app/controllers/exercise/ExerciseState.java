package org.igye.remem3.app.controllers.exercise;

public sealed interface ExerciseState permits SelectExerciseState, RunningExerciseState {
}
