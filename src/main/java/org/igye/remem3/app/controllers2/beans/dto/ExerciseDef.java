package org.igye.remem3.app.controllers2.beans.dto;

import lombok.Data;
import lombok.SneakyThrows;

import java.io.File;
import java.util.List;

public sealed interface ExerciseDef permits ExerciseDef.BaseExerciseDef {
    String getName();

    List<String> getDirectories();

    List<String> getRepeatStrategyTypes();


    @Data
    sealed abstract class BaseExerciseDef implements ExerciseDef permits SimpleExerciseDef {
        private String name;
    }

    @Data
    final class SimpleExerciseDef extends BaseExerciseDef {
        private List<File> dirs;
        private TaskFilter taskFilter;
        private RepeatStrategyParams repeatStrategy;

        @Override
        public List<String> getDirectories() {
            return dirs.stream().map(this::getCanonicalPath).distinct().sorted().toList();
        }

        @Override
        public List<String> getRepeatStrategyTypes() {
            return List.of(repeatStrategy.getRepeatStrategyType().toString());
        }

        @SneakyThrows
        private String getCanonicalPath(File file) {
            return file.getCanonicalPath();
        }
    }
}
