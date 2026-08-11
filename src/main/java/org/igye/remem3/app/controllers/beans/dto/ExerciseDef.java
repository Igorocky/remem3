package org.igye.remem3.app.controllers.beans.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public sealed interface ExerciseDef permits ExerciseDef.BaseExerciseDef {
    String getName();

    List<String> getDirectories();

    List<String> getRepeatStrategyTypes();


    @Data
    sealed abstract class BaseExerciseDef implements ExerciseDef permits SimpleExerciseDef, CompoundExerciseDef {
        private String name;
    }

    @EqualsAndHashCode(callSuper = true)
    @Data
    final class SimpleExerciseDef extends BaseExerciseDef {
        private List<File> dirs;
        private TaskFilter taskFilter;
        private RepeatStrategyParams repeatStrategy;
        private Optional<Instant> startTime = Optional.empty();

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

        public Instant getStartTime() {
            return startTime.orElseGet(() -> repeatStrategy.getStartTime().get());
        }

        public void setStartTime(Instant startTime) {
            this.startTime = Optional.of(startTime);
        }
    }

    @Getter
    final class CompoundExerciseDef extends BaseExerciseDef {
        private final List<Pair<Integer, ExerciseDef>> exercises;

        public CompoundExerciseDef(List<List<Object>> exercises) {
            this.exercises = exercises.stream()
                .map(e -> Pair.of((Integer) e.getFirst(), (ExerciseDef) e.get(1)))
                .toList();
        }

        @Override
        public List<String> getDirectories() {
            return exercises.stream()
                .map(Pair::getRight)
                .map(ExerciseDef::getDirectories)
                .flatMap(Collection::stream)
                .distinct()
                .sorted()
                .toList();
        }

        @Override
        public List<String> getRepeatStrategyTypes() {
            return exercises.stream()
                .map(Pair::getRight)
                .map(ExerciseDef::getRepeatStrategyTypes)
                .flatMap(Collection::stream)
                .distinct()
                .sorted()
                .toList();
        }
    }
}
