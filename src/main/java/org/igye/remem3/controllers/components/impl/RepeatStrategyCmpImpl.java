package org.igye.remem3.controllers.components.impl;

import org.apache.commons.lang3.tuple.Pair;
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
import org.igye.remem3.utils.Exn;
import org.igye.remem3.utils.Utils;
import org.igye.remem3.web.RequestParams;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.igye.remem3.app.impl.RepeatStrategyCircle.MAX_NUM_OF_ROUNDS;

public class RepeatStrategyCmpImpl extends HtmlBuilder implements RepeatStrategyCmp {

    private final Settings settings;
    private final Utils utils;
    private final String baseParamName;

    private final String parStrategyType;
    private RepeatStrategyType valStrategyType;

    private final String parCircleRndFactor;
    private double valCircleRndFactor;

    private final String parCircleNumOfRounds;
    private Optional<Integer> valCircleNumOfRounds;

    private final String parBucketsDelays;
    private String valBucketsDelays;

    private final String parBucketsUseBucketForNewTasks;
    private boolean valBucketsUseBucketForNewTasks;

    public RepeatStrategyCmpImpl(Settings settings, Utils utils, String baseParamName, RequestParams params) {
        this.settings = settings;
        this.utils = utils;
        this.baseParamName = baseParamName;
        parStrategyType = makeParamName("TYPE");
        parCircleRndFactor = makeParamName("CIRCLE_RANDOMNESS_FACTOR");
        parCircleNumOfRounds = makeParamName("CIRCLE_NUMBER_OF_ROUNDS");
        parBucketsDelays = makeParamName("BUCKETS_DELAYS");
        parBucketsUseBucketForNewTasks = makeParamName("BUCKETS_USE_BUCKET_FOR_NEW_TASKS");

        valStrategyType = params.hasParam(parStrategyType)
            ? RepeatStrategyType.valueOf(params.getParam(parStrategyType))
            : RepeatStrategyType.CIRCLE;

        int _ = switch (valStrategyType) {
            case CIRCLE -> {
                valCircleRndFactor = params.hasParam(parCircleRndFactor)
                    ? parseCircleRandomnessFactor(params.getParam(parCircleRndFactor))
                    : 0.3;
                valCircleNumOfRounds = params.hasParam(parCircleNumOfRounds)
                    ? parseCircleNumOfRounds(params.getParam(parCircleNumOfRounds))
                    : Optional.empty();
                yield 1;
            }
            case BUCKETS -> {
                valBucketsDelays = params.hasParam(parBucketsDelays)
                    ? params.getParam(parBucketsDelays)
                    : settings.getBucketDelays().getFirst().getName();
                valBucketsUseBucketForNewTasks =
                    !params.hasParam(parBucketsUseBucketForNewTasks)
                        || Boolean.parseBoolean(params.getParam(parBucketsUseBucketForNewTasks));
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
        return frag(
            table(List.of(List.of(
                text("Repeat strategy"),
                select(
                    parStrategyType,
                    true,
                    valStrategyType.toString(),
                    Arrays.stream(RepeatStrategyType.values())
                        .map(RepeatStrategyType::toString)
                        .map(typ -> Pair.of(typ, text(typ)))
                        .toList()
                )
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
            case CIRCLE -> new RepeatStrategyCircle(tasks, Instant.now(), valCircleRndFactor, valCircleNumOfRounds);
            case BUCKETS -> new RepeatStrategyBuckets(
                utils, Clock.systemDefaultZone(), tasks, getSelectedBucketDelays(), valBucketsUseBucketForNewTasks
            );
        };
    }

    @Override
    public List<Pair<String, String>> getProperties() {
        ArrayList<Pair<String, String>> props = new ArrayList<>();
        props.add(Pair.of("repeat_strategy", valStrategyType.toString()));
        int _ = switch (valStrategyType) {
            case CIRCLE -> {
                props.add(Pair.of("rounds", valCircleNumOfRounds.map(String::valueOf).orElse("")));
                props.add(Pair.of("randomness", String.valueOf(valCircleRndFactor)));
                yield 0;
            }
            case BUCKETS -> {
                props.add(Pair.of(
                    "bucket_delays",
                    getSelectedBucketDelays().stream()
                        .map(utils::durationToStr)
                        .collect(Collectors.joining(", "))
                ));
                props.add(Pair.of("use_separate_bucket_for_new_tasks", valBucketsUseBucketForNewTasks ? "y" : "n"));
                yield 0;
            }
        };
        return props;
    }

    private HtmlElem rndBucketsParams() {
        return table(List.of(
            List.of(
                text("Bucket delays"),
                select(parBucketsDelays, valBucketsDelays,
                    settings.getBucketDelays().stream()
                        .map(delays ->
                            Pair.of(
                                delays.getName(),
                                text(String.format("%s: %s", delays.getName(), delays.getDelaysFromProps()))
                            ))
                        .toList()
                )
            ),
            List.of(
                text("Use a separate bucket for new tasks"),
                select(parBucketsUseBucketForNewTasks, valBucketsUseBucketForNewTasks + "",
                    List.of(
                        Pair.of(Boolean.TRUE.toString(), text("Yes")),
                        Pair.of(Boolean.FALSE.toString(), text("No"))
                    )
                )
            )
        ));
    }

    private List<Duration> getSelectedBucketDelays() {
        return settings.getBucketDelays().stream()
            .filter(delays -> delays.getName().equals(valBucketsDelays))
            .findFirst()
            .map(BucketDelaysDto::getDelays)
            .orElseThrow(() -> new Exn(String.format("Cannot find bucket delays by name '%s'", valBucketsDelays)));
    }

    private HtmlElem rndCircleParams() {
        return table(List.of(
            List.of(
                text("Rounds"),
                inpText(parCircleNumOfRounds, valCircleNumOfRounds.map(String::valueOf).orElse(""), null)
            ),
            List.of(
                text("Randomness"),
                inpText(parCircleRndFactor, String.valueOf(valCircleRndFactor), null)
            )
        ));
    }

    private Optional<Integer> parseCircleNumOfRounds(String intStr) {
        try {
            return Optional.of(Math.max(1, Math.min(Integer.parseInt(intStr), MAX_NUM_OF_ROUNDS)));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private double parseCircleRandomnessFactor(String doubleStr) {
        try {
            return Math.max(0, Math.min(Double.parseDouble(doubleStr), 1));
        } catch (NumberFormatException e) {
            return 0.3;
        }
    }

    private String makeParamName(String suffix) {
        return baseParamName + "__" + suffix;
    }
}
