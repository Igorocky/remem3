package org.igye.remem3.controllers.exercise;

public sealed interface ExerciseAction {

    final class StartExercise implements ExerciseAction {
    }

    final class CancelExercise implements ExerciseAction {
    }
}
