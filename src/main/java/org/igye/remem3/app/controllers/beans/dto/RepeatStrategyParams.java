package org.igye.remem3.app.controllers.beans.dto;

import lombok.Data;
import org.igye.remem3.app.dto.RepeatStrategyType;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public sealed interface RepeatStrategyParams
    permits RepeatStrategyParams.RepeatStrategyBucketsParams, RepeatStrategyParams.RepeatStrategyCircleParams,
    RepeatStrategyParams.RepeatStrategyRetryFailedParams, RepeatStrategyParams.RepeatStrategyQueueParams {

    RepeatStrategyType getRepeatStrategyType();

    @Data
    final class RepeatStrategyBucketsParams implements RepeatStrategyParams {
        private List<Duration> delays;
        private int batchSize = 5;

        @Override
        public RepeatStrategyType getRepeatStrategyType() {
            return RepeatStrategyType.BUCKETS;
        }
    }

    @Data
    final class RepeatStrategyCircleParams implements RepeatStrategyParams {
        private Instant startTime = Instant.now();
        private Optional<Integer> rounds = Optional.empty();
        private double randomness = 0.3;

        @Override
        public RepeatStrategyType getRepeatStrategyType() {
            return RepeatStrategyType.CIRCLE;
        }
    }

    @Data
    final class RepeatStrategyRetryFailedParams implements RepeatStrategyParams {
        private Instant startTime = Instant.now();
        private double randomness = 0.3;

        @Override
        public RepeatStrategyType getRepeatStrategyType() {
            return RepeatStrategyType.RETRY_FAILED;
        }
    }

    @Data
    final class RepeatStrategyQueueParams implements RepeatStrategyParams {
        private Instant startTime = Instant.MIN;
        private int step = 5;
        private int batchSize = 5;

        @Override
        public RepeatStrategyType getRepeatStrategyType() {
            return RepeatStrategyType.QUEUE;
        }
    }
}
