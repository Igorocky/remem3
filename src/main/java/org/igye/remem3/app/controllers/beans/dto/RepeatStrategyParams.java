package org.igye.remem3.app.controllers.beans.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.igye.remem3.app.dto.RepeatStrategyType;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public sealed interface RepeatStrategyParams
    permits RepeatStrategyParams.RepeatStrategyBucketsParams, RepeatStrategyParams.RepeatStrategyCircleParams,
    RepeatStrategyParams.RepeatStrategyRetryFailedParams, RepeatStrategyParams.RepeatStrategyQueueParams {

    Supplier<Instant> getStartTime();

    RepeatStrategyType getRepeatStrategyType();

    @Getter
    @Builder
    @ToString
    final class RepeatStrategyBucketsParams implements RepeatStrategyParams {
        @Builder.Default
        private final Supplier<Instant> startTime = () -> Instant.MIN;
        private final List<Duration> delays;
        @Builder.Default
        private final int batchSize = 5;

        @Override
        public RepeatStrategyType getRepeatStrategyType() {
            return RepeatStrategyType.BUCKETS;
        }
    }

    @Getter
    @Builder
    @ToString
    final class RepeatStrategyCircleParams implements RepeatStrategyParams {
        @Builder.Default
        private final Supplier<Instant> startTime = Instant::now;
        @Builder.Default
        private final Optional<Integer> rounds = Optional.empty();
        @Builder.Default
        private final double randomness = 0.3;

        @Override
        public RepeatStrategyType getRepeatStrategyType() {
            return RepeatStrategyType.CIRCLE;
        }
    }

    @Getter
    @Builder
    @ToString
    final class RepeatStrategyRetryFailedParams implements RepeatStrategyParams {
        @Builder.Default
        private final Supplier<Instant> startTime = Instant::now;
        @Builder.Default
        private final double randomness = 0.3;

        @Override
        public RepeatStrategyType getRepeatStrategyType() {
            return RepeatStrategyType.RETRY_FAILED;
        }
    }

    @Getter
    @Builder
    @ToString
    final class RepeatStrategyQueueParams implements RepeatStrategyParams {
        @Builder.Default
        private final Supplier<Instant> startTime = () -> Instant.MIN;
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
    }
}
