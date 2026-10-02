package org.igye.remem3.app.impl;

import org.apache.commons.lang3.tuple.Pair;
import org.devtoolsgroup.simplespelshell.impl.FileSystemAwareSpelShellImpl;
import org.igye.remem3.app.Shell;
import org.igye.remem3.app.controllers.beans.dto.ExerciseDef;
import org.igye.remem3.app.controllers.beans.dto.RepeatStrategyParams;
import org.igye.remem3.app.controllers.beans.dto.TaskFilter;
import org.igye.remem3.utils.Exn;

import java.nio.file.Path;
import java.util.List;

public class ShellImpl extends FileSystemAwareSpelShellImpl implements Shell {

    public ShellImpl(Path initDir) {
        super(initDir);
        getWorkingDirectory().setCurrentDirectoryValidator(initDir, newDir -> {
            if (!initDir.toAbsolutePath().normalize().equals(newDir.toAbsolutePath().normalize())) {
                throw new Exn(
                    "Cannot change current directory to %s. The only allowed working directory is %s.".formatted(
                        newDir, initDir
                    )
                );
            }
        });
    }

    @Override
    public List<Pair<String, Object>> getBeans() {
        return getSpelEvaluator().getAllVariables().entrySet().stream()
            .filter(entry -> !"$".equals(entry.getKey()))
            .map(entry -> Pair.of(entry.getKey(), entry.getValue()))
            .toList();
    }

    @Override
    public <T> List<Pair<String, T>> getBeans(Class<T> type) {
        return getSpelEvaluator().getAllVariables().entrySet().stream()
            .filter(entry -> !"$".equals(entry.getKey()))
            .filter(entry -> type.isAssignableFrom(entry.getValue().getClass()))
            .map(entry -> Pair.of(entry.getKey(), (T) entry.getValue()))
            .toList();
    }

    @Override
    public <T> Pair<String, T> getBean(Class<T> type) {
        List<Pair<String, T>> found = getBeans(type);
        if (found.size() != 1) {
            throw new Exn("Expected to find exactly 1 bean of type %s, but found %s".formatted(type, found.size()));
        }
        return found.getFirst();
    }

    @Override
    public TaskFilter taskFilter(TaskFilter taskFilter) {
        return taskFilter;
    }

    @Override
    public RepeatStrategyParams.RepeatStrategyBucketsParams.RepeatStrategyBucketsParamsBuilder repeatStrategyBuckets() {
        return RepeatStrategyParams.RepeatStrategyBucketsParams.builder();
    }

    @Override
    public RepeatStrategyParams.RepeatStrategyCircleParams.RepeatStrategyCircleParamsBuilder repeatStrategyCircle() {
        return RepeatStrategyParams.RepeatStrategyCircleParams.builder();
    }

    @Override
    public RepeatStrategyParams.RepeatStrategyRetryFailedParams.RepeatStrategyRetryFailedParamsBuilder repeatStrategyRetryFailed() {
        return RepeatStrategyParams.RepeatStrategyRetryFailedParams.builder();
    }

    @Override
    public RepeatStrategyParams.RepeatStrategyQueueParams.RepeatStrategyQueueParamsBuilder repeatStrategyQueue() {
        return RepeatStrategyParams.RepeatStrategyQueueParams.builder();
    }

    @Override
    public ExerciseDef.SimpleExerciseDef.SimpleExerciseDefBuilder exercise() {
        return ExerciseDef.SimpleExerciseDef.builder();
    }

    @Override
    public ExerciseDef.CompoundExerciseDef.CompoundExerciseDefBuilder compoundExercise() {
        return ExerciseDef.CompoundExerciseDef.builder();
    }
}
