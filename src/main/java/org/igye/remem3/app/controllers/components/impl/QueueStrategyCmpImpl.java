package org.igye.remem3.app.controllers.components.impl;

import org.apache.commons.lang3.tuple.Pair;
import org.igye.remem3.app.Cache;
import org.igye.remem3.app.RepeatStrategyType;
import org.igye.remem3.app.dto.Task;
import org.igye.remem3.app.repeatstrategy.RepeatStrategy;
import org.igye.remem3.app.repeatstrategy.impl.RepeatStrategyQueue;
import org.igye.remem3.app.controllers.ParamName;
import org.igye.remem3.app.controllers.PropName;
import org.igye.remem3.html.HtmlElem;
import org.igye.remem3.html.HtmlTag;
import org.igye.remem3.utils.Utils;
import org.igye.remem3.web.RequestParams;

import java.io.File;
import java.time.Instant;
import java.util.List;
import java.util.Properties;

public class QueueStrategyCmpImpl extends BaseStrategyCmpImpl {
    private static final String PAR_BATCH_SIZE = "BATCH_SIZE";
    private static final String PAR_STEP = "STEP";
    private static final PropName PROP_BATCH_SIZE = new PropName("batch_size");
    private static final PropName PROP_STEP = new PropName("step");

    private final Utils utils;

    private final ParamName parBatchSize;
    private int valBatchSize;

    private final ParamName parStep;
    private int valStep;

    public QueueStrategyCmpImpl(
        Utils utils, Cache cache,
        String baseParamName,
        boolean isReadonly
    ) {
        super(cache, baseParamName + "__QUEUE", isReadonly);
        this.utils = utils;
        this.parBatchSize = makeParamName(PAR_BATCH_SIZE);
        this.parStep = makeParamName(PAR_STEP);
    }

    public QueueStrategyCmpImpl(Utils utils, Cache cache, String baseParamName, RequestParams params) {
        this(utils, cache, baseParamName, false);
        valBatchSize = readCachableParam(parBatchSize, params, this::parseBatchSize,
            () -> RepeatStrategyQueue.DEFAULT_BATCH_SIZE
        );
        valStep = readCachableParam(parStep, params, this::parseStep, () -> RepeatStrategyQueue.DEFAULT_STEP);
    }

    public QueueStrategyCmpImpl(Utils utils, Cache cache, String baseParamName, File configFile, Properties props) {
        this(utils, cache, baseParamName, true);
        valBatchSize = readProp(PROP_BATCH_SIZE, configFile, props, this::parseBatchSize,
            () -> RepeatStrategyQueue.DEFAULT_BATCH_SIZE
        );
        valStep = readProp(PROP_STEP, configFile, props, this::parseStep, () -> RepeatStrategyQueue.DEFAULT_STEP);
    }

    @Override
    public RepeatStrategyType getStrategyType() {
        return RepeatStrategyType.QUEUE;
    }

    @Override
    public HtmlElem render() {
        HtmlTag batchSize = inpText(parBatchSize.name(), String.valueOf(valBatchSize), null).attr("size", "3");
        if (isReadonly) {
            batchSize.disabled();
        }
        HtmlTag step = inpText(parStep.name(), String.valueOf(valStep), null).attr("size", "3");
        if (isReadonly) {
            step.disabled();
        }
        return table(List.of(
            List.of(text("Batch size"), batchSize),
            List.of(text("Step"), step)
        ));
    }

    @Override
    public RepeatStrategy makeRepeatStrategy(List<Task> tasks) {
        return new RepeatStrategyQueue(utils, Instant.now(), valBatchSize, valStep, makeTasksForStrategy(tasks));
    }

    @Override
    protected List<Pair<PropName, String>> getPropertiesPriv() {
        return List.of(
            Pair.of(PROP_BATCH_SIZE, String.valueOf(valBatchSize)),
            Pair.of(PROP_STEP, String.valueOf(valStep))
        );
    }

    @Override
    protected List<Pair<ParamName, String>> getParamsToCache() {
        return List.of(
            Pair.of(parBatchSize, String.valueOf(valBatchSize)),
            Pair.of(parStep, String.valueOf(valStep))
        );
    }

    private int parseBatchSize(String str) {
        return utils.getInRange(
            RepeatStrategyQueue.MIN_BATCH_SIZE,
            Integer.parseInt(str),
            RepeatStrategyQueue.MAX_BATCH_SIZE
        );
    }

    private int parseStep(String str) {
        return utils.getInRange(
            RepeatStrategyQueue.MIN_STEP,
            Integer.parseInt(str),
            RepeatStrategyQueue.MAX_STEP
        );
    }
}
