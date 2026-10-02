package org.igye.remem3.app.controllers.beans.dto;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.SuperBuilder;
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


    @Getter
    @RequiredArgsConstructor
    @SuperBuilder
    sealed abstract class BaseExerciseDef implements ExerciseDef permits SimpleExerciseDef, CompoundExerciseDef {
        private final String name;
    }

    @EqualsAndHashCode(callSuper = true)
    @Getter
    @SuperBuilder
    final class SimpleExerciseDef extends BaseExerciseDef {
        private final List<File> dirs;
        private final TaskFilter taskFilter;
        private final RepeatStrategyParams repeatStrategy;
        @Builder.Default
        private final Optional<Instant> startTime = Optional.empty();

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
    }

    @EqualsAndHashCode(callSuper = true)
    @Getter
    @SuperBuilder
    final class CompoundExerciseDef extends BaseExerciseDef {
        private final List<Pair<Integer, ExerciseDef>> exercises;

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
