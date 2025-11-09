package org.igye.remem3.app.controllers.components.impl;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.igye.remem3.app.Cache;
import org.igye.remem3.app.RepeatStrategyType;
import org.igye.remem3.app.Settings;
import org.igye.remem3.app.dto.BucketDelaysDto;
import org.igye.remem3.app.dto.Task;
import org.igye.remem3.app.repeatstrategy.RepeatStrategy;
import org.igye.remem3.app.repeatstrategy.impl.RepeatStrategyBuckets;
import org.igye.remem3.app.controllers.ParamName;
import org.igye.remem3.app.controllers.PropName;
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
    private static final String PAR_BATCH_SIZE = "BATCH_SIZE";
    private static final PropName PROP_BUCKET_DELAYS = new PropName("bucket_delays");
    private static final PropName PROP_BATCH_SIZE = new PropName("batch_size");

    private final Settings settings;
    private final Utils utils;

    private final ParamName parDelaysName;
    private String valDelaysName;
    private List<Duration> valDelaysList;

    private final ParamName parBatchSize;
    private int valBatchSize;

    public BucketsStrategyCmpImpl(
        Settings settings, Utils utils, Cache cache,
        String baseParamName,
        boolean isReadonly
    ) {
        super(cache, baseParamName + "__BUCKETS", isReadonly);
        this.settings = settings;
        this.utils = utils;
        this.parDelaysName = makeParamName(PAR_DELAYS_NAME);
        this.parBatchSize = makeParamName(PAR_BATCH_SIZE);
    }

    public BucketsStrategyCmpImpl(
        Settings settings, Utils utils, Cache cache, String baseParamName, RequestParams params
    ) {
        this(settings, utils, cache, baseParamName, false);
        valDelaysName = readCachableParam(parDelaysName, params, this::parseDelaysName, () -> parseDelaysName(""));
        valDelaysList = null;
        valBatchSize = readCachableParam(parBatchSize, params, this::parseBatchSize,
            () -> RepeatStrategyBuckets.DEFAULT_BATCH_SIZE
        );
    }

    public BucketsStrategyCmpImpl(
        Settings settings, Utils utils, Cache cache, String baseParamName, File configFile, Properties props
    ) {
        this(settings, utils, cache, baseParamName, true);
        valDelaysName = null;
        valDelaysList = readProp(PROP_BUCKET_DELAYS, configFile, props, utils::parseDurations, null);
        valBatchSize = readProp(PROP_BATCH_SIZE, configFile, props, this::parseBatchSize,
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
            delaysSelector = select(parDelaysName.name(), valDelaysName,
                settings.getBucketDelays().stream()
                    .map(delays ->
                        Pair.of(
                            delays.getName(),
                            text(format("%s: %s", delays.getName(), delays.getDelaysFromProps()))
                        ))
                    .toList()
            );
        }
        HtmlTag batchSize = inpText(parBatchSize.name(), String.valueOf(valBatchSize), null).attr("size", "3");
        if (isReadonly) {
            batchSize.disabled();
        }
        return table(List.of(
            List.of(text("Bucket delays"), delaysSelector),
            List.of(text("Batch size"), batchSize)
        ));
    }

    @Override
    public RepeatStrategy makeRepeatStrategy(List<Task> tasks) {
        return new RepeatStrategyBuckets(
            utils, Clock.systemDefaultZone(), valBatchSize, makeTasksForStrategy(tasks), getSelectedBucketDelays()
        );
    }

    @Override
    protected List<Pair<PropName, String>> getPropertiesPriv() {
        return List.of(
            Pair.of(
                PROP_BUCKET_DELAYS,
                getSelectedBucketDelays().stream()
                    .map(utils::durationToStr)
                    .collect(Collectors.joining(", "))
            ),
            Pair.of(PROP_BATCH_SIZE, String.valueOf(valBatchSize))
        );
    }

    @Override
    protected List<Pair<ParamName, String>> getParamsToCache() {
        return List.of(
            Pair.of(parDelaysName, valDelaysName),
            Pair.of(parBatchSize, String.valueOf(valBatchSize))
        );
    }

    private String parseDelaysName(String str) {
        return settings.getBucketDelays().stream().map(BucketDelaysDto::getName).anyMatch(str::equals)
            ? str
            : settings.getBucketDelays().getFirst().getName();
    }

    private int parseBatchSize(String str) {
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
