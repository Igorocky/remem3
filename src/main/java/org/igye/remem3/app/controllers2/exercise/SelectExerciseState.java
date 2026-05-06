package org.igye.remem3.app.controllers2.exercise;

import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.With;
import org.igye.remem3.app.controllers2.beans.dto.ExerciseDef;

import java.util.List;

@AllArgsConstructor
@Builder
@Getter
public final class SelectExerciseState implements ExerciseState {
    private final List<ExerciseDef> allExercises;
    @With
    @Nullable
    private final ExerciseDef selectedExercise;
}
