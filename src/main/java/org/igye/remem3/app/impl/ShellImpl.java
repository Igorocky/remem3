package org.igye.remem3.app.impl;

import org.apache.commons.lang3.tuple.Pair;
import org.devtoolsgroup.simplespelshell.SpelEvaluator;
import org.devtoolsgroup.simplespelshell.impl.FileSystemAwareSpelShellImpl;
import org.igye.remem3.app.CardUtils;
import org.igye.remem3.app.Shell;
import org.igye.remem3.app.controllers.beans.converter.DurationConverter;
import org.igye.remem3.app.controllers.beans.converter.InstantConverter;
import org.igye.remem3.app.controllers.beans.converter.OperatorOverloaderImpl;
import org.igye.remem3.app.controllers.beans.converter.TaskFilterConverter;
import org.igye.remem3.app.controllers.beans.converter.TaskTypeMatcherConverter;
import org.igye.remem3.app.controllers.beans.dto.ExerciseDef;
import org.igye.remem3.app.controllers.beans.dto.RepeatStrategyParams;
import org.igye.remem3.app.controllers.beans.dto.TaskFilter;
import org.igye.remem3.app.controllers.newcard.CardDto;
import org.igye.remem3.app.dto.Card;
import org.igye.remem3.utils.Exn;
import org.igye.remem3.utils.Utils;
import org.springframework.core.convert.converter.Converter;

import java.io.File;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ShellImpl extends FileSystemAwareSpelShellImpl implements Shell {
    private final Utils utils;
    private final CardUtils cardUtils;

    public ShellImpl(Path initDir, Utils utils, CardUtils cardUtils) {
        super(initDir);
        this.utils = utils;
        this.cardUtils = cardUtils;
        SpelEvaluator spelEvaluator = getSpelEvaluator();
        List<Converter<?, ?>> typeConverters = new ArrayList<>(spelEvaluator.getTypeConverters());
        typeConverters.add(new InstantConverter());
        typeConverters.add(new TaskTypeMatcherConverter());
        typeConverters.add(new TaskFilterConverter(this));
        typeConverters.add(new DurationConverter(utils));
        spelEvaluator.setTypeConverters(typeConverters);
        spelEvaluator.setOperatorOverloader(new OperatorOverloaderImpl(spelEvaluator.getConversionService()));
    }

    @Override
    public Shell shell() {
        return this;
    }

    @Override
    public List<Pair<String, Object>> getBeans() {
        return getSpelEvaluator().getAllVariables().entrySet().stream()
            .filter(entry -> !getLastEvalResultVarName().equals(entry.getKey()))
            .map(entry -> Pair.of(entry.getKey(), entry.getValue()))
            .toList();
    }

    @Override
    public <T> List<Pair<String, T>> getBeans(Class<T> type) {
        return getSpelEvaluator().getAllVariables().entrySet().stream()
            .filter(entry -> !getLastEvalResultVarName().equals(entry.getKey()))
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
    public Instant instant(Instant instant) {
        return instant;
    }

    @Override
    public <T> Supplier<T> supplier(T obj) {
        return () -> obj;
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
    public ExerciseDef.SimpleExerciseDef.SimpleExerciseDefBuilder exercise(String name) {
        return ExerciseDef.SimpleExerciseDef.builder().name(name);
    }

    @Override
    public ExerciseDef.CompoundExerciseDef.CompoundExerciseDefBuilder compoundExercise() {
        return ExerciseDef.CompoundExerciseDef.builder();
    }

    @Override
    public CardDto.FillGaps.FillGapsBuilder cardDtoFillGaps() {
        return CardDto.FillGaps.builder();
    }

    @Override
    public CardDto.Translate.TranslateBuilder cardDtoTranslate() {
        return CardDto.Translate.builder();
    }

    @Override
    public Object parseJson(String jsonStr) {
        return utils.parseJson(jsonStr, Object.class);
    }

    @Override
    public Card makeCard(CardDto cardDto) {
        return cardUtils.makeCard(cardDto);
    }

    @Override
    public void saveCardInNewFile(File dir, Card card) {
        cardUtils.saveCard(new File(dir, cardUtils.makeFileNameForCard(card)), card);
    }
}
