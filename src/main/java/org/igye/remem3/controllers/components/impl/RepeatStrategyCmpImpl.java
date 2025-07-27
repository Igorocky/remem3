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

import static org.igye.remem3.app.impl.RepeatStrategyCircle.MAX_NUM_OF_ROUNDS;

public class RepeatStrategyCmpImpl extends HtmlBuilder implements RepeatStrategyCmp {

    private static final String PROP_REPEAT_STRATEGY = "repeat_strategy";
    private static final String PROP_CIRCLE_ROUNDS = "rounds";
    private static final String PROP_CIRCLE_RANDOMNESS = "randomness";
    private static final String PROP_BUCKETS_BUCKET_DELAYS = "bucket_delays";
    private static final String PROP_BUCKETS_USE_SEPARATE_BUCKET_FOR_NEW_TASKS = "use_separate_bucket_for_new_tasks";
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

    public RepeatStrategyCmpImpl(
        Settings settings, Utils utils, Cache cache, String baseParamName, RequestParams params
    ) {
        this.settings = settings;
        this.utils = utils;
        this.cache = cache;
        this.baseParamName = baseParamName;
        isReadonly = false;
        parStrategyType = makeParamName("TYPE");
        parCircleRndFactor = makeParamName("CIRCLE_RANDOMNESS_FACTOR");
        parCircleNumOfRounds = makeParamName("CIRCLE_NUMBER_OF_ROUNDS");
        parBucketsDelaysName = makeParamName("BUCKETS_DELAYS_NAME");
        parBucketsUseBucketForNewTasks = makeParamName("BUCKETS_USE_BUCKET_FOR_NEW_TASKS");

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
        parStrategyType = makeParamName("TYPE");
        parCircleRndFactor = makeParamName("CIRCLE_RANDOMNESS_FACTOR");
        parCircleNumOfRounds = makeParamName("CIRCLE_NUMBER_OF_ROUNDS");
        parBucketsDelaysName = makeParamName("BUCKETS_DELAYS");
        parBucketsUseBucketForNewTasks = makeParamName("BUCKETS_USE_BUCKET_FOR_NEW_TASKS");

        String strategyTypeStr = props.getProperty(PROP_REPEAT_STRATEGY);
        if (StringUtils.isBlank(strategyTypeStr)) {
            throw new Exn(String.format(
                "%s is not specified in %s.", PROP_REPEAT_STRATEGY, configFile.getAbsolutePath()
            ));
        }
        try {
            valStrategyType = RepeatStrategyType.valueOf(strategyTypeStr);
        } catch (IllegalArgumentException e) {
            throw new Exn(String.format(
                "Cannot parse %s=%s in %s.", PROP_REPEAT_STRATEGY, strategyTypeStr, configFile.getAbsolutePath()
            ));
        }

        int _ = switch (valStrategyType) {
            case CIRCLE -> {
                String numOfRoundsStr = props.getProperty(PROP_CIRCLE_ROUNDS);
                valCircleNumOfRounds = StringUtils.isNotBlank(numOfRoundsStr)
                    ? parseCircleNumOfRounds(numOfRoundsStr)
                    : Optional.empty();
                String rndFactorStr = props.getProperty(PROP_CIRCLE_RANDOMNESS);
                valCircleRndFactor = StringUtils.isNotBlank(rndFactorStr)
                    ? parseCircleRandomnessFactor(rndFactorStr)
                    : new BigDecimal("0.3");
                yield 1;
            }
            case BUCKETS -> {
                String bucketsDelaysStr = props.getProperty(PROP_BUCKETS_BUCKET_DELAYS);
                try {
                    valBucketsDelaysName = null;
                    valBucketsDelaysList = utils.parseDurations(bucketsDelaysStr);
                    if (valBucketsDelaysList.isEmpty()) {
                        throw new Exn("At least one bucket must be specified.");
                    }
                } catch (Exception e) {
                    throw new Exn(String.format(
                        "Cannot parse %s=%s in %s, got an error %s.",
                        PROP_BUCKETS_BUCKET_DELAYS,
                        bucketsDelaysStr,
                        configFile.getAbsolutePath(),
                        e.getMessage()
                    ));
                }
                String useBucketForNewTasksStr = props.getProperty(PROP_BUCKETS_USE_SEPARATE_BUCKET_FOR_NEW_TASKS);
                valBucketsUseBucketForNewTasks =
                    StringUtils.isBlank(useBucketForNewTasksStr) || "y".equals(useBucketForNewTasksStr.trim());
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
                utils, Clock.systemDefaultZone(), tasks, getSelectedBucketDelays(), valBucketsUseBucketForNewTasks
            );
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
                yield 1;
            }
        };
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
                            text(String.format("%s: %s", delays.getName(), delays.getDelaysFromProps()))
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
        return table(List.of(
            List.of(text("Bucket delays"), delaysSelector),
            List.of(text("Use a separate bucket for new tasks"), bucketForNewTasksSelector)
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
            .orElseThrow(() -> new Exn(String.format("Cannot find bucket delays by name '%s'", valBucketsDelaysName)));
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

    private BigDecimal parseCircleRandomnessFactor(String str) {
        try {
            BigDecimal res = new BigDecimal(str);
            if (res.compareTo(BigDecimal.ZERO) < 0) {
                return BigDecimal.ZERO;
            }
            if (BigDecimal.ONE.compareTo(res) < 0) {
                return BigDecimal.ONE;
            }
            return res;
        } catch (Exception e) {
            return new BigDecimal("0.3");
        }
    }

    private String makeParamName(String suffix) {
        return baseParamName + "__" + suffix;
    }
}
