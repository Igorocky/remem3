package org.igye.remem3.controllers.components.impl;

import org.apache.commons.lang3.tuple.Pair;
import org.igye.remem3.app.Cache;
import org.igye.remem3.app.RepeatStrategyType;
import org.igye.remem3.app.dto.Task;
import org.igye.remem3.app.repeatstrategy.RepeatStrategy;
import org.igye.remem3.app.repeatstrategy.impl.RepeatStrategyQueue;
import org.igye.remem3.html.HtmlElem;
import org.igye.remem3.html.HtmlTag;
import org.igye.remem3.utils.Utils;
import org.igye.remem3.web.RequestParams;

import java.io.File;
import java.util.List;
import java.util.Properties;

public class QueueStrategyCmpImpl extends BaseStrategyCmpImpl {
    private static final String PAR_BATCH_SIZE = "BATCH_SIZE";
    private static final String PAR_STEP = "STEP";
    private static final String PROP_BATCH_SIZE = "batch_size";
    private static final String PROP_STEP = "step";

    private final Utils utils;
    private final Cache cache;

    private final String parBatchSize;
    private int valBatchSize;

    private final String parStep;
    private int valStep;

    public QueueStrategyCmpImpl(
        Utils utils, Cache cache,
        String baseParamName,
        boolean isReadonly
    ) {
        super(baseParamName + "__QUEUE", isReadonly);
        this.utils = utils;
        this.cache = cache;
        this.parBatchSize = makeParamName(PAR_BATCH_SIZE);
        this.parStep = makeParamName(PAR_STEP);
    }

    public QueueStrategyCmpImpl(Utils utils, Cache cache, String baseParamName, RequestParams params) {
        this(utils, cache, baseParamName, false);
        valBatchSize = readParam(params, parBatchSize, this::parseBatchSizeExn,
            () -> RepeatStrategyQueue.DEFAULT_BATCH_SIZE
        );
        valStep = readParam(params, parStep, this::parseStepExn,
            () -> RepeatStrategyQueue.DEFAULT_STEP
        );
    }

    public QueueStrategyCmpImpl(Utils utils, Cache cache, String baseParamName, File configFile, Properties props) {
        this(utils, cache, baseParamName, true);
        valBatchSize = readPropExn(configFile, props, PROP_BATCH_SIZE, this::parseBatchSizeExn,
            () -> RepeatStrategyQueue.DEFAULT_BATCH_SIZE
        );
        valStep = readPropExn(configFile, props, PROP_STEP, this::parseStepExn,
            () -> RepeatStrategyQueue.DEFAULT_STEP
        );
    }

    @Override
    public RepeatStrategyType getStrategyType() {
        return RepeatStrategyType.QUEUE;
    }

    @Override
    public HtmlElem render() {
        HtmlTag batchSize = inpText(parBatchSize, String.valueOf(valBatchSize), null).attr("size", "3");
        if (isReadonly) {
            batchSize.disabled();
        }
        HtmlTag step = inpText(parStep, String.valueOf(valStep), null).attr("size", "3");
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
        return new RepeatStrategyQueue(utils, valBatchSize, valStep, makeTasksForStrategy(tasks));
    }

    @Override
    public List<Pair<String, String>> getProperties() {
        return List.of(
            Pair.of(PROP_BATCH_SIZE, String.valueOf(valBatchSize)),
            Pair.of(PROP_STEP, String.valueOf(valStep))
        );
    }

    @Override
    public void cacheState() {
        cache.put(parBatchSize, valBatchSize);
        cache.put(parStep, valStep);
    }

    private int parseBatchSizeExn(String str) {
        return utils.getInRange(
            RepeatStrategyQueue.MIN_BATCH_SIZE,
            Integer.parseInt(str),
            RepeatStrategyQueue.MAX_BATCH_SIZE
        );
    }

    private int parseStepExn(String str) {
        return utils.getInRange(
            RepeatStrategyQueue.MIN_STEP,
            Integer.parseInt(str),
            RepeatStrategyQueue.MAX_STEP
        );
    }
}
