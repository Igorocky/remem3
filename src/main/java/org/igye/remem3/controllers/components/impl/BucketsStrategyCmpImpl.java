package org.igye.remem3.controllers.components.impl;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.igye.remem3.app.Cache;
import org.igye.remem3.app.RepeatStrategyType;
import org.igye.remem3.app.Settings;
import org.igye.remem3.app.dto.BucketDelaysDto;
import org.igye.remem3.app.dto.Task;
import org.igye.remem3.app.repeatstrategy.RepeatStrategy;
import org.igye.remem3.app.repeatstrategy.impl.RepeatStrategyBuckets;
import org.igye.remem3.html.HtmlElem;
import org.igye.remem3.html.HtmlTag;
import org.igye.remem3.utils.Exn;
import org.igye.remem3.utils.Utils;
import org.igye.remem3.web.RequestParams;

import java.io.File;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class BucketsStrategyCmpImpl extends BaseStrategyCmpImpl {
    private static final String PAR_DELAYS_NAME = "DELAYS_NAME";
    private static final String PAR_USE_BUCKET_FOR_NEW_TASKS = "USE_BUCKET_FOR_NEW_TASKS";
    private static final String PAR_BATCH_SIZE = "BATCH_SIZE";
    private static final String PROP_BUCKET_DELAYS = "bucket_delays";
    private static final String PROP_USE_SEPARATE_BUCKET_FOR_NEW_TASKS = "use_separate_bucket_for_new_tasks";
    private static final String PROP_BATCH_SIZE = "batch_size";

    private final Settings settings;
    private final Utils utils;
    private final Cache cache;

    private final String parDelaysName;
    private String valDelaysName;
    private List<Duration> valDelaysList;

    private final String parUseBucketForNewTasks;
    private boolean valUseBucketForNewTasks;

    private final String parBatchSize;
    private int valBatchSize;

    public BucketsStrategyCmpImpl(
        Settings settings, Utils utils, Cache cache,
        String baseParamName,
        boolean isReadonly
    ) {
        super(baseParamName + "__BUCKETS", isReadonly);
        this.settings = settings;
        this.utils = utils;
        this.cache = cache;
        this.parDelaysName = makeParamName(PAR_DELAYS_NAME);
        this.parUseBucketForNewTasks = makeParamName(PAR_USE_BUCKET_FOR_NEW_TASKS);
        this.parBatchSize = makeParamName(PAR_BATCH_SIZE);
    }

    public BucketsStrategyCmpImpl(
        Settings settings, Utils utils, Cache cache, String baseParamName, RequestParams params
    ) {
        this(settings, utils, cache, baseParamName, false);
        valDelaysName = readParam(params, parDelaysName, this::parseDelaysName,
            () -> parseDelaysName(cache.getStr(parDelaysName, ""))
        );
        valDelaysList = null;
        valUseBucketForNewTasks = readParam(params, parUseBucketForNewTasks, Boolean::parseBoolean, () -> true);
        valBatchSize = readParam(params, parBatchSize, this::parseBatchSizeExn,
            () -> RepeatStrategyBuckets.DEFAULT_BATCH_SIZE
        );
    }

    public BucketsStrategyCmpImpl(
        Settings settings, Utils utils, Cache cache, String baseParamName, File configFile, Properties props
    ) {
        this(settings, utils, cache, baseParamName, true);
        valDelaysName = null;
        valDelaysList = readPropExn(configFile, props, PROP_BUCKET_DELAYS, utils::parseDurations, null);
        valUseBucketForNewTasks = readPropExn(configFile, props, PROP_USE_SEPARATE_BUCKET_FOR_NEW_TASKS,
            Boolean::parseBoolean, () -> true
        );
        valBatchSize = readPropExn(configFile, props, PROP_BATCH_SIZE, this::parseBatchSizeExn,
            () -> RepeatStrategyBuckets.DEFAULT_BATCH_SIZE
        );
    }

    @Override
    public RepeatStrategyType getStrategyType() {
        return RepeatStrategyType.BUCKETS;
    }

    @Override
    public HtmlElem render() {
        HtmlElem delaysSelector;
        if (isReadonly) {
            delaysSelector = text(
                valDelaysList.stream().map(utils::durationToStr).collect(Collectors.joining(", "))
            );
        } else {
            delaysSelector = select(parDelaysName, valDelaysName,
                settings.getBucketDelays().stream()
                    .map(delays ->
                        Pair.of(
                            delays.getName(),
                            text(format("%s: %s", delays.getName(), delays.getDelaysFromProps()))
                        ))
                    .toList()
            );
        }
        HtmlTag bucketForNewTasksSelector = select(parUseBucketForNewTasks, valUseBucketForNewTasks + "",
            List.of(
                Pair.of(Boolean.TRUE.toString(), text("Yes")),
                Pair.of(Boolean.FALSE.toString(), text("No"))
            )
        );
        if (isReadonly) {
            bucketForNewTasksSelector.disabled();
        }
        HtmlTag batchSize = inpText(parBatchSize, String.valueOf(valBatchSize), null).attr("size", "3");
        if (isReadonly) {
            batchSize.disabled();
        }
        return table(List.of(
            List.of(text("Bucket delays"), delaysSelector),
            List.of(text("Use a separate bucket for new tasks"), bucketForNewTasksSelector),
            List.of(text("Batch size"), batchSize)
        ));
    }

    @Override
    public RepeatStrategy makeRepeatStrategy(List<Task> tasks) {
        return new RepeatStrategyBuckets(
            utils, Clock.systemDefaultZone(),
            valBatchSize, false,
            makeTasksForStrategy(tasks), getSelectedBucketDelays(), valUseBucketForNewTasks
        );
    }

    @Override
    public List<Pair<String, String>> getProperties() {
        return List.of(
            Pair.of(
                PROP_BUCKET_DELAYS,
                getSelectedBucketDelays().stream()
                    .map(utils::durationToStr)
                    .collect(Collectors.joining(", "))
            ),
            Pair.of(
                PROP_USE_SEPARATE_BUCKET_FOR_NEW_TASKS,
                valUseBucketForNewTasks ? "y" : "n"
            ),
            Pair.of(PROP_BATCH_SIZE, String.valueOf(valBatchSize))
        );
    }

    @Override
    public void cacheState() {
        cache.put(parDelaysName, valDelaysName);
        cache.put(parUseBucketForNewTasks, valUseBucketForNewTasks);
        cache.put(parBatchSize, valBatchSize);
    }

    private String parseDelaysName(String str) {
        return settings.getBucketDelays().stream().map(BucketDelaysDto::getName).anyMatch(str::equals)
            ? str
            : settings.getBucketDelays().getFirst().getName();
    }

    private int parseBatchSizeExn(String str) {
        return utils.getInRange(
            RepeatStrategyBuckets.MIN_BATCH_SIZE,
            Integer.parseInt(str),
            RepeatStrategyBuckets.MAX_BATCH_SIZE
        );
    }

    private List<Duration> getSelectedBucketDelays() {
        if (CollectionUtils.isNotEmpty(valDelaysList)) {
            return valDelaysList;
        }
        return settings.getBucketDelays().stream()
            .filter(delays -> delays.getName().equals(valDelaysName))
            .findFirst()
            .map(BucketDelaysDto::getDelays)
            .orElseThrow(() -> new Exn(format("Cannot find bucket delays by name '%s'", valDelaysName)));
    }
}
