package org.igye.remem3.app.controllers.exercise;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.With;
import org.apache.commons.lang3.tuple.Pair;
import org.igye.remem3.app.Cache;
import org.igye.remem3.app.CardUtils;
import org.igye.remem3.app.Settings;
import org.igye.remem3.app.controllers.components.DirSelectorCmp;
import org.igye.remem3.app.controllers.components.RepeatStrategyCmp;
import org.igye.remem3.app.dto.Task;
import org.igye.remem3.app.dto.TaskType;
import org.igye.remem3.app.repeatstrategy.RepeatStrategy;
import org.igye.remem3.app.task.TaskState;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public sealed interface ExerciseState permits ExerciseState.SetParams, ExerciseState.Started {
    List<String> getErrors();

    @Builder
    @Getter
    @With
    @EqualsAndHashCode
    @ToString
    final class SetParams implements ExerciseState {
        private List<String> errors;
        private Settings settings;
        private Cache cache;
        private CardUtils cardUtils;
        private String config;
        private DirSelectorCmp dirSelector;
        private List<Pair<TaskType, Boolean>> taskTypes;
        private RepeatStrategyCmp repeatStrategyCmp;
        private boolean showProperties;
    }

    @Builder
    @Getter
    @With
    @EqualsAndHashCode
    @ToString
    final class Started implements ExerciseState {
        private List<String> errors;
        private Settings settings;
        private Cache cache;
        private CardUtils cardUtils;
        private String config;
        private RepeatStrategyCmp repeatStrategyCmp;
        private String dir;
        private Set<String> taskTypes;
        private RepeatStrategy repeatStrategy;
        private Optional<Boolean> showMoreParams;
        private boolean showDailyUniqueCount;
        private Optional<List<Task>> nextTasks;
        private Optional<TaskState> taskState;
        private boolean cardPathCopied;
    }
}
