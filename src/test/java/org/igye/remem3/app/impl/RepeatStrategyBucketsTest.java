package org.igye.remem3.app.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.igye.remem3.app.dto.HistRec;
import org.igye.remem3.utils.Exn;
import org.igye.remem3.utils.impl.UtilsImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

import static java.lang.String.format;

class RepeatStrategyBucketsTest {

    private static final BigDecimal PRECISION = new BigDecimal("0.000001");

    @Test
    void getOverdue() {
        Instant curTime = Instant.now();
        RepeatStrategyBuckets strat = new RepeatStrategyBuckets(
            new UtilsImpl(new ObjectMapper()), null, 0, false, List.of(), List.of(), false
        );
        assertEquals(
            new BigDecimal("0"),
            strat.getOverdue(curTime, Duration.of(1, ChronoUnit.MINUTES), makeHist(curTime.minusSeconds(30)))
        );
        assertEquals(
            new BigDecimal("0"),
            strat.getOverdue(curTime, Duration.of(1, ChronoUnit.MINUTES), makeHist(curTime.minusSeconds(60)))
        );
        assertEquals(
            new BigDecimal("0.15"),
            strat.getOverdue(curTime, Duration.of(1, ChronoUnit.MINUTES), List.of())
        );
        assertEquals(
            new BigDecimal("0.1"),
            strat.getOverdue(curTime, Duration.of(1, ChronoUnit.MINUTES), makeHist(curTime.minusSeconds(66)))
        );
        assertEquals(
            new BigDecimal("0.2"),
            strat.getOverdue(curTime, Duration.of(1, ChronoUnit.MINUTES), makeHist(curTime.minusSeconds(72)))
        );
        assertEquals(
            new BigDecimal("0.5"),
            strat.getOverdue(curTime, Duration.of(1, ChronoUnit.MINUTES), makeHist(curTime.minusSeconds(90)))
        );
        assertEquals(
            new BigDecimal("1"),
            strat.getOverdue(curTime, Duration.of(1, ChronoUnit.MINUTES), makeHist(curTime.minusSeconds(120)))
        );
        assertEquals(
            new BigDecimal("2"),
            strat.getOverdue(curTime, Duration.of(1, ChronoUnit.MINUTES), makeHist(curTime.minusSeconds(180)))
        );
    }

    @Test
    void getBucketNum() {
        RepeatStrategyBuckets strat = new RepeatStrategyBuckets(
            new UtilsImpl(new ObjectMapper()), null, 0, false, List.of(), List.of(), false
        );
        Assertions.assertEquals(
            0,
            strat.getBucketNum(makeHist(BigDecimal.ZERO))
        );
        Assertions.assertEquals(
            0,
            strat.getBucketNum(makeHist(BigDecimal.ZERO, BigDecimal.ZERO))
        );
        Assertions.assertEquals(
            0,
            strat.getBucketNum(makeHist(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO))
        );
        Assertions.assertEquals(
            0,
            strat.getBucketNum(makeHist(BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO))
        );
        Assertions.assertEquals(
            0,
            strat.getBucketNum(makeHist(BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ZERO))
        );
        Assertions.assertEquals(
            0,
            strat.getBucketNum(makeHist(BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.ZERO))
        );
        Assertions.assertEquals(
            1,
            strat.getBucketNum(makeHist(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ONE))
        );
        Assertions.assertEquals(
            2,
            strat.getBucketNum(makeHist(BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.ONE))
        );
        Assertions.assertEquals(
            3,
            strat.getBucketNum(makeHist(BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE))
        );
        Assertions.assertEquals(
            2,
            strat.getBucketNum(makeHist(BigDecimal.ONE, BigDecimal.ONE))
        );
        Assertions.assertEquals(
            1,
            strat.getBucketNum(makeHist(BigDecimal.ONE))
        );
    }

    private void assertEquals(BigDecimal expected, BigDecimal actual) {
        if (!(
            expected.subtract(PRECISION).compareTo(actual) <= 0
                && actual.compareTo(expected.add(PRECISION)) <= 0
        )) {
            throw new Exn(format("Expected: %s, Actual: %s", expected, actual));
        }
    }

    private List<HistRec> makeHist(Instant time) {
        return List.of(HistRec.builder().time(time).build());
    }

    private List<HistRec> makeHist(BigDecimal... marks) {
        return Arrays.stream(marks).map(mark -> HistRec.builder().mark(mark).build()).toList();
    }

}