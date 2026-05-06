package org.igye.remem3.app.controllers2.beans.dto;

import lombok.Getter;
import lombok.SneakyThrows;
import lombok.experimental.SuperBuilder;

import java.io.File;
import java.util.List;

public sealed interface ExerciseDef permits ExerciseDef.BaseExerciseDef {
    String getName();

    List<String> getDirectories();

    List<String> getRepeatStrategyTypes();

    @SuperBuilder
    @Getter
    sealed abstract class BaseExerciseDef implements ExerciseDef permits SimpleExerciseDef {
        private final String name;
    }

    @SuperBuilder
    @Getter
    final class SimpleExerciseDef extends BaseExerciseDef {
        private final List<File> dirs;
        private final TaskFilter taskFilter;
        private final RepeatStrategyParams repeatStrategyParams;

        @Override
        public List<String> getDirectories() {
            return dirs.stream().map(this::getCanonicalPath).distinct().sorted().toList();
        }

        @Override
        public List<String> getRepeatStrategyTypes() {
            return List.of(repeatStrategyParams.getRepeatStrategyType().toString());
        }

        @SneakyThrows
        private String getCanonicalPath(File file) {
            return file.getCanonicalPath();
        }
    }
}
