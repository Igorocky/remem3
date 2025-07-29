package org.igye.remem3.controllers.components.impl;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.igye.remem3.app.Cache;
import org.igye.remem3.app.RepeatStrategy;
import org.igye.remem3.app.RepeatStrategyType;
import org.igye.remem3.app.Settings;
import org.igye.remem3.app.dto.BucketDelaysDto;
import org.igye.remem3.app.dto.Task;
import org.igye.remem3.app.impl.RepeatStrategyBuckets;
import org.igye.remem3.app.impl.RepeatStrategyCircle;
import org.igye.remem3.app.impl.RepeatStrategyQueue;
import org.igye.remem3.controllers.components.RepeatStrategyCmp;
import org.igye.remem3.html.HtmlBuilder;
import org.igye.remem3.html.HtmlElem;
import org.igye.remem3.html.HtmlTag;
import org.igye.remem3.utils.Exn;
import org.igye.remem3.utils.Utils;
import org.igye.remem3.web.RequestParams;

import java.io.File;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.igye.remem3.app.impl.RepeatStrategyBuckets.DEFAULT_BATCH_SIZE;
import static org.igye.remem3.app.impl.RepeatStrategyBuckets.MAX_BATCH_SIZE;
import static org.igye.remem3.app.impl.RepeatStrategyBuckets.MIN_BATCH_SIZE;
import static org.igye.remem3.app.impl.RepeatStrategyCircle.MAX_NUM_OF_ROUNDS;

public class RepeatStrategyCmpImpl extends HtmlBuilder implements RepeatStrategyCmp {

    private static final String PAR_TYPE = "TYPE";
    private static final String PAR_CIRCLE_RANDOMNESS_FACTOR = "CIRCLE_RANDOMNESS_FACTOR";
    private static final String PAR_CIRCLE_NUMBER_OF_ROUNDS = "CIRCLE_NUMBER_OF_ROUNDS";
    private static final String PAR_BUCKETS_DELAYS_NAME = "BUCKETS_DELAYS_NAME";
    private static final String PAR_BUCKETS_USE_BUCKET_FOR_NEW_TASKS = "BUCKETS_USE_BUCKET_FOR_NEW_TASKS";
    private static final String PAR_BUCKETS_BATCH_SIZE = "BUCKETS_BATCH_SIZE";
    private static final String PAR_BUCKETS_PREFER_TASKS_WITH_LONGER_HISTORY =
        "BUCKETS_PREFER_TASKS_WITH_LONGER_HISTORY";
    private static final String PAR_QUEUE_BATCH_SIZE = "QUEUE_BATCH_SIZE";

    private static final String PROP_REPEAT_STRATEGY = "repeat_strategy";
    private static final String PROP_CIRCLE_ROUNDS = "rounds";
    private static final String PROP_CIRCLE_RANDOMNESS = "randomness";
    private static final String PROP_BUCKETS_BUCKET_DELAYS = "bucket_delays";
    private static final String PROP_BUCKETS_USE_SEPARATE_BUCKET_FOR_NEW_TASKS = "use_separate_bucket_for_new_tasks";
    private static final String PROP_BUCKETS_PREFER_TASKS_WITH_LONGER_HISTORY = "prefer_tasks_with_longer_history";
    private static final String PROP_BUCKETS_BATCH_SIZE = "batch_size";
    private static final String PROP_QUEUE_BATCH_SIZE = "batch_size";

    private final Settings settings;
    private final Utils utils;
    private final Cache cache;
    private final String baseParamName;
    private final boolean isReadonly;

    private final String parStrategyType;
    private RepeatStrategyType valStrategyType;

    private final String parCircleNumOfRounds;
    private Optional<Integer> valCircleNumOfRounds;

    private final String parCircleRndFactor;
    private BigDecimal valCircleRndFactor;

    private final String parBucketsDelaysName;
    private String valBucketsDelaysName;
    private List<Duration> valBucketsDelaysList;

    private final String parBucketsUseBucketForNewTasks;
    private boolean valBucketsUseBucketForNewTasks;

    private final String parBucketsPreferTasksWithLongerHistory;
    private boolean valBucketsPreferTasksWithLongerHistory;

    private final String parBucketsBatchSize;
    private int valBucketsBatchSize;

    private final String parQueueBatchSize;
    private int valQueueBatchSize;

    public RepeatStrategyCmpImpl(
        Settings settings, Utils utils, Cache cache, String baseParamName, RequestParams params
    ) {
        this.settings = settings;
        this.utils = utils;
        this.cache = cache;
        this.baseParamName = baseParamName;
        isReadonly = false;
        parStrategyType = makeParamName(PAR_TYPE);
        parCircleRndFactor = makeParamName(PAR_CIRCLE_RANDOMNESS_FACTOR);
        parCircleNumOfRounds = makeParamName(PAR_CIRCLE_NUMBER_OF_ROUNDS);
        parBucketsDelaysName = makeParamName(PAR_BUCKETS_DELAYS_NAME);
        parBucketsUseBucketForNewTasks = makeParamName(PAR_BUCKETS_USE_BUCKET_FOR_NEW_TASKS);
        parBucketsBatchSize = makeParamName(PAR_BUCKETS_BATCH_SIZE);
        parBucketsPreferTasksWithLongerHistory = makeParamName(PAR_BUCKETS_PREFER_TASKS_WITH_LONGER_HISTORY);
        parQueueBatchSize = makeParamName(PAR_QUEUE_BATCH_SIZE);

        valStrategyType = params.hasParam(parStrategyType)
            ? RepeatStrategyType.valueOf(params.getParam(parStrategyType))
            : RepeatStrategyType.valueOf(cache.getStr(parStrategyType, RepeatStrategyType.CIRCLE.toString()));

        int _ = switch (valStrategyType) {
            case CIRCLE -> {
                valCircleNumOfRounds = params.hasParam(parCircleNumOfRounds)
                    ? parseCircleNumOfRounds(params.getParam(parCircleNumOfRounds))
                    : Optional.of(cache.getStr(parCircleNumOfRounds, ""))
                    .flatMap(str -> StringUtils.isBlank(str) ? Optional.empty() : parseCircleNumOfRounds(str));
                valCircleRndFactor = params.hasParam(parCircleRndFactor)
                    ? parseCircleRandomnessFactor(params.getParam(parCircleRndFactor))
                    : parseCircleRandomnessFactor(cache.getStr(parCircleRndFactor, ""));
                yield 1;
            }
            case BUCKETS -> {
                valBucketsDelaysName = params.hasParam(parBucketsDelaysName)
                    ? params.getParam(parBucketsDelaysName)
                    : cache.getStr(parBucketsDelaysName, settings.getBucketDelays().getFirst().getName());
                if (
                    settings.getBucketDelays().stream()
                        .map(BucketDelaysDto::getName)
                        .noneMatch(name -> name.equals(valBucketsDelaysName))
                ) {
                    valBucketsDelaysName = settings.getBucketDelays().getFirst().getName();
                }
                valBucketsDelaysList = null;
                valBucketsUseBucketForNewTasks = params.hasParam(parBucketsUseBucketForNewTasks)
                    ? Boolean.parseBoolean(params.getParam(parBucketsUseBucketForNewTasks))
                    : cache.getBool(parBucketsUseBucketForNewTasks, true);
                valBucketsPreferTasksWithLongerHistory = params.hasParam(parBucketsPreferTasksWithLongerHistory)
                    ? Boolean.parseBoolean(params.getParam(parBucketsPreferTasksWithLongerHistory))
                    : cache.getBool(parBucketsPreferTasksWithLongerHistory, false);
                valBucketsBatchSize = params.hasParam(parBucketsBatchSize)
                    ? utils.getInRange(
                    MIN_BATCH_SIZE,
                    parseBatchSize(params.getParam(parBucketsBatchSize), DEFAULT_BATCH_SIZE),
                    MAX_BATCH_SIZE
                )
                    : cache.getInt(parBucketsBatchSize, DEFAULT_BATCH_SIZE);
                yield 1;
            }
            case QUEUE -> {
                valQueueBatchSize = params.hasParam(parQueueBatchSize)
                    ? utils.getInRange(
                    RepeatStrategyQueue.MIN_BATCH_SIZE,
                    parseBatchSize(params.getParam(parQueueBatchSize), RepeatStrategyQueue.DEFAULT_BATCH_SIZE),
                    RepeatStrategyQueue.MAX_BATCH_SIZE
                )
                    : cache.getInt(parQueueBatchSize, RepeatStrategyQueue.DEFAULT_BATCH_SIZE);
                yield 1;
            }
        };
    }

    public RepeatStrategyCmpImpl(
        Settings settings, Utils utils, Cache cache, String baseParamName, File configFile, Properties props
    ) {
        this.settings = settings;
        this.utils = utils;
        this.cache = cache;
        this.baseParamName = baseParamName;
        isReadonly = true;
        parStrategyType = makeParamName(PAR_TYPE);
        parCircleRndFactor = makeParamName(PAR_CIRCLE_RANDOMNESS_FACTOR);
        parCircleNumOfRounds = makeParamName(PAR_CIRCLE_NUMBER_OF_ROUNDS);
        parBucketsDelaysName = makeParamName(PAR_BUCKETS_DELAYS_NAME);
        parBucketsUseBucketForNewTasks = makeParamName(PAR_BUCKETS_USE_BUCKET_FOR_NEW_TASKS);
        parBucketsBatchSize = makeParamName(PAR_BUCKETS_BATCH_SIZE);
        parBucketsPreferTasksWithLongerHistory = makeParamName(PAR_BUCKETS_PREFER_TASKS_WITH_LONGER_HISTORY);
        parQueueBatchSize = makeParamName(PAR_QUEUE_BATCH_SIZE);

        String strategyTypeStr = props.getProperty(PROP_REPEAT_STRATEGY);
        if (StringUtils.isBlank(strategyTypeStr)) {
            throw new Exn(format(
                "%s is not specified in %s.", PROP_REPEAT_STRATEGY, configFile.getAbsolutePath()
            ));
        }
        try {
            valStrategyType = RepeatStrategyType.valueOf(strategyTypeStr);
        } catch (IllegalArgumentException e) {
            throw new Exn(format(
                "Cannot parse %s=%s in %s.", PROP_REPEAT_STRATEGY, strategyTypeStr, configFile.getAbsolutePath()
            ));
        }

        int _ = switch (valStrategyType) {
            case CIRCLE -> {
                valCircleNumOfRounds = readCircleNumOfRoundsFromProps(configFile, props);
                valCircleRndFactor = readCircleRndFactorFromProps(configFile, props);
                yield 1;
            }
            case BUCKETS -> {
                valBucketsDelaysName = null;
                valBucketsDelaysList = readBucketsDelaysListFromProps(configFile, props);
                valBucketsUseBucketForNewTasks = readBucketsUseBucketForNewTasksFromProps(configFile, props);
                valBucketsPreferTasksWithLongerHistory = readBucketsPreferTasksWithLongerHistoryFromProps(
                    configFile, props
                );
                valBucketsBatchSize = readBucketsBatchSizeFromProps(configFile, props);
                yield 1;
            }
            case QUEUE -> {
                valQueueBatchSize = readQueueBatchSizeFromProps(configFile, props);
                yield 1;
            }
        };
    }

    @Override
    public RepeatStrategyType getStrategyType() {
        return valStrategyType;
    }

    @Override
    public HtmlElem render() {
        HtmlTag strategySelector = select(
            parStrategyType,
            true,
            valStrategyType.toString(),
            Arrays.stream(RepeatStrategyType.values())
                .map(RepeatStrategyType::toString)
                .map(typ -> Pair.of(typ, text(typ)))
                .toList()
        );
        if (isReadonly) {
            strategySelector.disabled();
        }
        return frag(
            table(List.of(List.of(
                text("Repeat strategy"),
                strategySelector
            ))),
            switch (valStrategyType) {
                case CIRCLE -> rndCircleParams();
                case BUCKETS -> rndBucketsParams();
                case QUEUE -> rndQueueParams();
            }
        );
    }

    @Override
    public RepeatStrategy makeRepeatStrategy(List<Task> tasks) {
        return switch (valStrategyType) {
            case CIRCLE -> new RepeatStrategyCircle(
                tasks, Instant.now(), valCircleRndFactor.doubleValue(), valCircleNumOfRounds
            );
            case BUCKETS -> new RepeatStrategyBuckets(
                utils, Clock.systemDefaultZone(),
                valBucketsBatchSize, valBucketsPreferTasksWithLongerHistory,
                tasks, getSelectedBucketDelays(), valBucketsUseBucketForNewTasks
            );
            case QUEUE -> new RepeatStrategyQueue(utils, valQueueBatchSize, 7, tasks);
        };
    }

    @Override
    public List<Pair<String, String>> getProperties() {
        ArrayList<Pair<String, String>> props = new ArrayList<>();
        props.add(Pair.of(PROP_REPEAT_STRATEGY, valStrategyType.toString()));
        int _ = switch (valStrategyType) {
            case CIRCLE -> {
                props.add(Pair.of(PROP_CIRCLE_ROUNDS, valCircleNumOfRounds.map(String::valueOf).orElse("")));
                props.add(Pair.of(PROP_CIRCLE_RANDOMNESS, String.valueOf(valCircleRndFactor)));
                yield 0;
            }
            case BUCKETS -> {
                props.add(Pair.of(
                    PROP_BUCKETS_BUCKET_DELAYS,
                    getSelectedBucketDelays().stream()
                        .map(utils::durationToStr)
                        .collect(Collectors.joining(", "))
                ));
                props.add(Pair.of(
                    PROP_BUCKETS_USE_SEPARATE_BUCKET_FOR_NEW_TASKS,
                    valBucketsUseBucketForNewTasks ? "y" : "n"
                ));
                props.add(Pair.of(
                    PROP_BUCKETS_PREFER_TASKS_WITH_LONGER_HISTORY,
                    valBucketsPreferTasksWithLongerHistory ? "y" : "n"
                ));
                props.add(Pair.of(PROP_BUCKETS_BATCH_SIZE, String.valueOf(valBucketsBatchSize)));
                yield 0;
            }
            case QUEUE -> {
                props.add(Pair.of(PROP_QUEUE_BATCH_SIZE, String.valueOf(valQueueBatchSize)));
                yield 0;
            }
        };
        return props;
    }

    @Override
    public void cacheState() {
        cache.put(parStrategyType, valStrategyType.toString());
        int _ = switch (valStrategyType) {
            case CIRCLE -> {
                cache.put(parCircleNumOfRounds, valCircleNumOfRounds.map(String::valueOf).orElse(""));
                cache.put(parCircleRndFactor, String.valueOf(valCircleRndFactor));
                yield 1;
            }
            case BUCKETS -> {
                cache.put(parBucketsDelaysName, valBucketsDelaysName);
                cache.put(parBucketsUseBucketForNewTasks, valBucketsUseBucketForNewTasks);
                cache.put(parBucketsPreferTasksWithLongerHistory, valBucketsPreferTasksWithLongerHistory);
                cache.put(parBucketsBatchSize, valBucketsBatchSize);
                yield 1;
            }
            case QUEUE -> {
                cache.put(parQueueBatchSize, valQueueBatchSize);
                yield 1;
            }
        };
    }

    private Optional<Integer> readCircleNumOfRoundsFromProps(File configFile, Properties props) {
        String numOfRoundsStr = props.getProperty(PROP_CIRCLE_ROUNDS);
        if (StringUtils.isBlank(numOfRoundsStr)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Math.max(1, Math.min(Integer.parseInt(numOfRoundsStr), MAX_NUM_OF_ROUNDS)));
        } catch (Exception e) {
            throw new Exn(format(
                "Cannot parse %s=%s in %s, got an error %s.",
                PROP_CIRCLE_ROUNDS,
                numOfRoundsStr,
                configFile.getAbsolutePath(),
                e.getMessage()
            ));
        }
    }

    private BigDecimal readCircleRndFactorFromProps(File configFile, Properties props) {
        String rndFactorStr = props.getProperty(PROP_CIRCLE_RANDOMNESS);
        try {
            return parseCircleRandomnessFactor(rndFactorStr);
        } catch (Exception e) {
            throw new Exn(format(
                "Cannot parse %s=%s in %s, got an error %s.",
                PROP_CIRCLE_RANDOMNESS,
                rndFactorStr,
                configFile.getAbsolutePath(),
                e.getMessage()
            ));
        }
    }

    private int readBucketsBatchSizeFromProps(File configFile, Properties props) {
        String valBucketsBatchSizeStr = props.getProperty(PROP_BUCKETS_BATCH_SIZE);
        if (StringUtils.isBlank(valBucketsBatchSizeStr)) {
            return DEFAULT_BATCH_SIZE;
        } else {
            try {
                return utils.getInRange(MIN_BATCH_SIZE, Integer.parseInt(valBucketsBatchSizeStr), MAX_BATCH_SIZE);
            } catch (Exception e) {
                throw new Exn(format(
                    "Cannot parse %s=%s in %s, got an error %s.",
                    PROP_BUCKETS_BATCH_SIZE,
                    valBucketsBatchSizeStr,
                    configFile.getAbsolutePath(),
                    e.getMessage()
                ));
            }
        }
    }

    private int readQueueBatchSizeFromProps(File configFile, Properties props) {
        String valQueueBatchSizeStr = props.getProperty(PROP_QUEUE_BATCH_SIZE);
        if (StringUtils.isBlank(valQueueBatchSizeStr)) {
            return RepeatStrategyQueue.DEFAULT_BATCH_SIZE;
        } else {
            try {
                return utils.getInRange(
                    RepeatStrategyQueue.MIN_BATCH_SIZE,
                    Integer.parseInt(valQueueBatchSizeStr),
                    RepeatStrategyQueue.MAX_BATCH_SIZE
                );
            } catch (Exception e) {
                throw new Exn(format(
                    "Cannot parse %s=%s in %s, got an error %s.",
                    PROP_QUEUE_BATCH_SIZE,
                    valQueueBatchSizeStr,
                    configFile.getAbsolutePath(),
                    e.getMessage()
                ));
            }
        }
    }

    private List<Duration> readBucketsDelaysListFromProps(File configFile, Properties props) {
        String bucketsDelaysStr = props.getProperty(PROP_BUCKETS_BUCKET_DELAYS);
        try {
            List<Duration> res = utils.parseDurations(bucketsDelaysStr);
            if (res.isEmpty()) {
                throw new Exn("At least one bucket must be specified.");
            }
            return res;
        } catch (Exception e) {
            throw new Exn(format(
                "Cannot parse %s=%s in %s, got an error %s.",
                PROP_BUCKETS_BUCKET_DELAYS,
                bucketsDelaysStr,
                configFile.getAbsolutePath(),
                e.getMessage()
            ));
        }
    }

    private boolean readBucketsUseBucketForNewTasksFromProps(File configFile, Properties props) {
        String useBucketForNewTasksStr = props.getProperty(PROP_BUCKETS_USE_SEPARATE_BUCKET_FOR_NEW_TASKS);
        if (StringUtils.isBlank(useBucketForNewTasksStr)) {
            return true;
        }
        useBucketForNewTasksStr = useBucketForNewTasksStr.trim().toLowerCase();
        if ("y".equals(useBucketForNewTasksStr)) {
            return true;
        }
        if ("n".equals(useBucketForNewTasksStr)) {
            return false;
        }
        throw new Exn(format(
            "%s must be empty or one of '%s', '%s', in %s",
            PROP_BUCKETS_USE_SEPARATE_BUCKET_FOR_NEW_TASKS, "y", "n", configFile.getAbsolutePath()
        ));
    }

    private boolean readBucketsPreferTasksWithLongerHistoryFromProps(File configFile, Properties props) {
        String preferTasksWithLongerHistoryStr = props.getProperty(PROP_BUCKETS_PREFER_TASKS_WITH_LONGER_HISTORY);
        if (StringUtils.isBlank(preferTasksWithLongerHistoryStr)) {
            return false;
        }
        preferTasksWithLongerHistoryStr = preferTasksWithLongerHistoryStr.trim().toLowerCase();
        if ("y".equals(preferTasksWithLongerHistoryStr)) {
            return true;
        }
        if ("n".equals(preferTasksWithLongerHistoryStr)) {
            return false;
        }
        throw new Exn(format(
            "%s must be empty or one of '%s', '%s', in %s",
            PROP_BUCKETS_PREFER_TASKS_WITH_LONGER_HISTORY, "y", "n", configFile.getAbsolutePath()
        ));
    }

    private int parseBatchSize(String str, int defaultBatchSize) {
        try {
            return Integer.parseInt(str);
        } catch (Exception ex) {
            return defaultBatchSize;
        }
    }

    private HtmlElem rndBucketsParams() {
        HtmlElem delaysSelector;
        if (isReadonly) {
            delaysSelector = text(
                valBucketsDelaysList.stream().map(utils::durationToStr).collect(Collectors.joining(", "))
            );
        } else {
            delaysSelector = select(parBucketsDelaysName, valBucketsDelaysName,
                settings.getBucketDelays().stream()
                    .map(delays ->
                        Pair.of(
                            delays.getName(),
                            text(format("%s: %s", delays.getName(), delays.getDelaysFromProps()))
                        ))
                    .toList()
            );
        }
        HtmlTag bucketForNewTasksSelector = select(parBucketsUseBucketForNewTasks, valBucketsUseBucketForNewTasks + "",
            List.of(
                Pair.of(Boolean.TRUE.toString(), text("Yes")),
                Pair.of(Boolean.FALSE.toString(), text("No"))
            )
        );
        if (isReadonly) {
            bucketForNewTasksSelector.disabled();
        }
        HtmlTag preferTasksWithLongerHistorySelector = select(
            parBucketsPreferTasksWithLongerHistory,
            valBucketsPreferTasksWithLongerHistory + "",
            List.of(
                Pair.of(Boolean.TRUE.toString(), text("Yes")),
                Pair.of(Boolean.FALSE.toString(), text("No"))
            )
        );
        if (isReadonly) {
            preferTasksWithLongerHistorySelector.disabled();
        }
        HtmlTag batchSize = inpText(parBucketsBatchSize, String.valueOf(valBucketsBatchSize), null).attr("size", "3");
        if (isReadonly) {
            batchSize.disabled();
        }
        return table(List.of(
            List.of(text("Bucket delays"), delaysSelector),
            List.of(text("Use a separate bucket for new tasks"), bucketForNewTasksSelector),
            List.of(text("Prefer tasks with longer history"), preferTasksWithLongerHistorySelector),
            List.of(text("Batch size"), batchSize)
        ));
    }

    private HtmlElem rndQueueParams() {
        HtmlTag batchSize = inpText(parQueueBatchSize, String.valueOf(valQueueBatchSize), null).attr("size", "3");
        if (isReadonly) {
            batchSize.disabled();
        }
        return table(List.of(
            List.of(text("Batch size"), batchSize)
        ));
    }

    private List<Duration> getSelectedBucketDelays() {
        if (CollectionUtils.isNotEmpty(valBucketsDelaysList)) {
            return valBucketsDelaysList;
        }
        return settings.getBucketDelays().stream()
            .filter(delays -> delays.getName().equals(valBucketsDelaysName))
            .findFirst()
            .map(BucketDelaysDto::getDelays)
            .orElseThrow(() -> new Exn(format("Cannot find bucket delays by name '%s'", valBucketsDelaysName)));
    }

    private HtmlElem rndCircleParams() {
        HtmlTag roundsInput = inpText(parCircleNumOfRounds, valCircleNumOfRounds.map(String::valueOf).orElse(""), null);
        if (isReadonly) {
            roundsInput.disabled();
        }
        HtmlTag randomnessInput = inpText(parCircleRndFactor, String.valueOf(valCircleRndFactor), null);
        if (isReadonly) {
            randomnessInput.disabled();
        }
        return table(List.of(
            List.of(text("Rounds"), roundsInput),
            List.of(text("Randomness"), randomnessInput)
        ));
    }

    private Optional<Integer> parseCircleNumOfRounds(String intStr) {
        try {
            return Optional.of(Math.max(1, Math.min(Integer.parseInt(intStr), MAX_NUM_OF_ROUNDS)));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private BigDecimal parseCircleRandomnessFactorExn(String str) {
        BigDecimal res = new BigDecimal(str);
        if (res.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        if (BigDecimal.ONE.compareTo(res) < 0) {
            return BigDecimal.ONE;
        }
        return res;
    }

    private BigDecimal parseCircleRandomnessFactor(String str) {
        try {
            return parseCircleRandomnessFactorExn(str);
        } catch (Exception e) {
            return new BigDecimal("0.3");
        }
    }

    private String makeParamName(String suffix) {
        return baseParamName + "__" + suffix;
    }
}
