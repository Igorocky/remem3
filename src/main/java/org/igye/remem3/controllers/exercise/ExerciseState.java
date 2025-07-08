package org.igye.remem3.controllers.exercise;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.With;
import org.apache.commons.lang3.tuple.Pair;
import org.igye.remem3.app.RepeatStrategy;
import org.igye.remem3.app.dto.Task;
import org.igye.remem3.app.dto.TaskType;
import org.igye.remem3.app.impl.CardsImpl;
import org.igye.remem3.controllers.components.DirSelectorCmp;
import org.igye.remem3.controllers.components.RepeatStrategyCmp;

import java.util.List;

public sealed interface ExerciseState {
    List<String> getErrors();

    @Builder
    @Getter
    @With
    @EqualsAndHashCode
    @ToString
    final class SetParams implements ExerciseState {
        private List<String> errors;
        private CardsImpl cards;
        private DirSelectorCmp dirSelector;
        private List<Pair<TaskType, Boolean>> taskTypes;
        private RepeatStrategyCmp repeatStrategyCmp;
    }

    @Builder
    @Getter
    @With
    @EqualsAndHashCode
    @ToString
    final class Started implements ExerciseState {
        private List<String> errors;
        private String dir;
        private List<String> taskTypes;
        private List<Task> tasks;
        private RepeatStrategy repeatStrategy;
    }
}
