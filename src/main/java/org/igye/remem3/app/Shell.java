package org.igye.remem3.app;

import org.apache.commons.lang3.tuple.Pair;
import org.devtoolsgroup.simplespelshell.FileSystemAwareSpelShell;
import org.igye.remem3.app.controllers.beans.dto.ExerciseDef;
import org.igye.remem3.app.controllers.beans.dto.RepeatStrategyParams;
import org.igye.remem3.app.controllers.beans.dto.TaskFilter;
import org.igye.remem3.app.controllers.newcard.CardDto;
import org.igye.remem3.app.dto.Card;

import java.io.File;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public interface Shell extends FileSystemAwareSpelShell {

    Shell shell();

    HashMap<?, ?> hashMap(Map<?, ?> map);

    CardDto.FillGaps.FillGapsBuilder cardDtoFillGaps();

    CardDto.Translate.TranslateBuilder cardDtoTranslate();

    Object parseJson(String jsonStr);

    Card makeCard(CardDto cardDto);

    void saveCardInNewFile(File dir, Card card);

    List<Pair<String, Object>> getBeans();

    <T> List<Pair<String, T>> getBeans(Class<T> type);

    <T> Pair<String, T> getBean(Class<T> type);

    TaskFilter taskFilter(TaskFilter taskFilter);

    Instant instant(Instant instant);

    <T> Supplier<T> supplier(T obj);

    RepeatStrategyParams.RepeatStrategyBucketsParams.RepeatStrategyBucketsParamsBuilder repeatStrategyBuckets();

    RepeatStrategyParams.RepeatStrategyCircleParams.RepeatStrategyCircleParamsBuilder repeatStrategyCircle();

    RepeatStrategyParams.RepeatStrategyRetryFailedParams.RepeatStrategyRetryFailedParamsBuilder repeatStrategyRetryFailed();

    RepeatStrategyParams.RepeatStrategyQueueParams.RepeatStrategyQueueParamsBuilder repeatStrategyQueue();

    ExerciseDef.SimpleExerciseDef.SimpleExerciseDefBuilder exercise(String name);

    ExerciseDef.CompoundExerciseDef.CompoundExerciseDefBuilder compoundExercise();
}
