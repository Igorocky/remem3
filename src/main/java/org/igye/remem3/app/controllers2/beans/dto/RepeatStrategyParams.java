package org.igye.remem3.app.controllers2.beans.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.igye.remem3.app.dto.RepeatStrategyType;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public sealed interface RepeatStrategyParams
    permits RepeatStrategyParams.RepeatStrategyBucketsParams, RepeatStrategyParams.RepeatStrategyCircleParams,
    RepeatStrategyParams.RepeatStrategyQueueParams {

    RepeatStrategyType getRepeatStrategyType();

    @RequiredArgsConstructor
    @Getter
    final class RepeatStrategyBucketsParams implements RepeatStrategyParams {
        private final List<Duration> bucketDelays;
        private final int batchSize;

        @Override
        public RepeatStrategyType getRepeatStrategyType() {
            return RepeatStrategyType.BUCKETS;
        }
    }

    @RequiredArgsConstructor
    @Getter
    final class RepeatStrategyCircleParams implements RepeatStrategyParams {
        private final Instant startTime;
        private final Optional<Integer> numOfRounds;
        private final double randomnessFactor;

        @Override
        public RepeatStrategyType getRepeatStrategyType() {
            return RepeatStrategyType.CIRCLE;
        }
    }

    @RequiredArgsConstructor
    @Getter
    final class RepeatStrategyQueueParams implements RepeatStrategyParams {
        private final Instant startTime;
        private final int step;
        private final int batchSize;

        @Override
        public RepeatStrategyType getRepeatStrategyType() {
            return RepeatStrategyType.QUEUE;
        }
    }
}
