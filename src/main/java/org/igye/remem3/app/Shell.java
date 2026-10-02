package org.igye.remem3.app;

import org.apache.commons.lang3.tuple.Pair;
import org.devtoolsgroup.simplespelshell.FileSystemAwareSpelShell;
import org.igye.remem3.app.controllers.beans.dto.ExerciseDef;
import org.igye.remem3.app.controllers.beans.dto.RepeatStrategyParams;
import org.igye.remem3.app.controllers.beans.dto.TaskFilter;

import java.util.List;

public interface Shell extends FileSystemAwareSpelShell {
    List<Pair<String, Object>> getBeans();

    <T> List<Pair<String, T>> getBeans(Class<T> type);

    <T> Pair<String, T> getBean(Class<T> type);

    TaskFilter taskFilter(TaskFilter taskFilter);

    RepeatStrategyParams.RepeatStrategyBucketsParams.RepeatStrategyBucketsParamsBuilder repeatStrategyBuckets();

    RepeatStrategyParams.RepeatStrategyCircleParams.RepeatStrategyCircleParamsBuilder repeatStrategyCircle();

    RepeatStrategyParams.RepeatStrategyRetryFailedParams.RepeatStrategyRetryFailedParamsBuilder repeatStrategyRetryFailed();

    RepeatStrategyParams.RepeatStrategyQueueParams.RepeatStrategyQueueParamsBuilder repeatStrategyQueue();

    ExerciseDef.SimpleExerciseDef.SimpleExerciseDefBuilder exercise();

    ExerciseDef.CompoundExerciseDef.CompoundExerciseDefBuilder compoundExercise();
}
