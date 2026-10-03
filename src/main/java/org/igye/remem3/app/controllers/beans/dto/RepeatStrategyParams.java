package org.igye.remem3.app.controllers.beans.dto;

import lombok.Builder;
import lombok.Getter;
import org.igye.remem3.app.dto.RepeatStrategyType;
import org.igye.remem3.utils.Utils;
import org.igye.remem3.utils.impl.UtilsImpl;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public sealed interface RepeatStrategyParams
    permits RepeatStrategyParams.RepeatStrategyBucketsParams, RepeatStrategyParams.RepeatStrategyCircleParams,
    RepeatStrategyParams.RepeatStrategyRetryFailedParams, RepeatStrategyParams.RepeatStrategyQueueParams {

    Supplier<Instant> INSTANT_SUPPLIER_NOW = Instant::now;
    Supplier<Instant> INSTANT_SUPPLIER_MIN = () -> Instant.MIN;

    Supplier<Instant> getStartTime();

    RepeatStrategyType getRepeatStrategyType();

    default String startTimeToString() {
        if (getStartTime() == RepeatStrategyParams.INSTANT_SUPPLIER_MIN) {
            return "MIN";
        } else if (getStartTime() == RepeatStrategyParams.INSTANT_SUPPLIER_NOW) {
            return "NOW";
        } else {
            return getStartTime().get().toString();
        }
    }

    @Getter
    @Builder
    final class RepeatStrategyBucketsParams implements RepeatStrategyParams {
        @Builder.Default
        private final Supplier<Instant> startTime = INSTANT_SUPPLIER_MIN;
        private final List<Duration> delays;
        @Builder.Default
        private final int batchSize = 5;

        private static Utils utils;

        @Override
        public RepeatStrategyType getRepeatStrategyType() {
            return RepeatStrategyType.BUCKETS;
        }

        @Override
        public String toString() {
            if (utils == null) {
                utils = new UtilsImpl(new ObjectMapper());
            }
            return "Buckets{" +
                "startTime=" + startTimeToString() +
                ", delays=" + delays.stream().map(utils::durationToStr).toList() +
                ", batchSize=" + batchSize +
                '}';
        }
    }

    @Getter
    @Builder
    final class RepeatStrategyCircleParams implements RepeatStrategyParams {
        @Builder.Default
        private final Supplier<Instant> startTime = INSTANT_SUPPLIER_NOW;
        @Builder.Default
        private final Optional<Integer> rounds = Optional.empty();
        @Builder.Default
        private final double randomness = 0.3;

        @Override
        public RepeatStrategyType getRepeatStrategyType() {
            return RepeatStrategyType.CIRCLE;
        }

        @Override
        public String toString() {
            return "Circle{" +
                "startTime=" + startTimeToString() +
                ", rounds=" + rounds +
                ", randomness=" + randomness +
                '}';
        }
    }

    @Getter
    @Builder
    final class RepeatStrategyRetryFailedParams implements RepeatStrategyParams {
        @Builder.Default
        private final Supplier<Instant> startTime = INSTANT_SUPPLIER_NOW;
        @Builder.Default
        private final double randomness = 0.3;

        @Override
        public RepeatStrategyType getRepeatStrategyType() {
            return RepeatStrategyType.RETRY_FAILED;
        }

        @Override
        public String toString() {
            return "RetryFailed{" +
                "startTime=" + startTimeToString() +
                ", randomness=" + randomness +
                '}';
        }
    }

    @Getter
    @Builder
    final class RepeatStrategyQueueParams implements RepeatStrategyParams {
        @Builder.Default
        private final Supplier<Instant> startTime = INSTANT_SUPPLIER_MIN;
        @Builder.Default
        private final int step = 5;
        @Builder.Default
        private final BigDecimal stepMultFactor = BigDecimal.TWO;
        @Builder.Default
        private final int batchSize = 5;
        @Builder.Default
        private final int maxHistLenStat = 20;

        @Override
        public RepeatStrategyType getRepeatStrategyType() {
            return RepeatStrategyType.QUEUE;
        }

        @Override
        public String toString() {
            return "Queue{" +
                "startTime=" + startTimeToString() +
                ", step=" + step +
                ", stepMultFactor=" + stepMultFactor +
                ", batchSize=" + batchSize +
                ", maxHistLenStat=" + maxHistLenStat +
                '}';
        }
    }
}
