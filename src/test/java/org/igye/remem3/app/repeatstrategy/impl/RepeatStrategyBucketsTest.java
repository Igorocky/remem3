package org.igye.remem3.app.repeatstrategy.impl;

import org.igye.remem3.app.dto.Card;
import org.igye.remem3.app.dto.RepeatStrategyType;
import org.igye.remem3.app.dto.Task;
import org.igye.remem3.app.repeatstrategy.HistRec;
import org.igye.remem3.utils.Exn;
import org.igye.remem3.utils.impl.UtilsImpl;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static java.lang.String.format;

class RepeatStrategyBucketsTest {

    private static final BigDecimal PRECISION = new BigDecimal("0.000001");

    @Test
    void calOverdue() {
        Instant curTime = Instant.now();
        Card.FillGaps dummyCard = Card.FillGaps.builder().build();
        TaskImpl dummyTask = new TaskImpl(
            new Task(dummyCard, dummyCard.getTaskTypes().getFirst()), Instant.MIN, RepeatStrategyType.CIRCLE
        );
        RepeatStrategyBuckets strat = new RepeatStrategyBuckets(
            new UtilsImpl(new ObjectMapper()), null, 0, List.of(dummyTask), List.of(Duration.of(1, ChronoUnit.MINUTES))
        );
        assertEquals(
            new BigDecimal("-0.5"),
            strat.calOverdue(
                curTime, strat.durToBigDec(Duration.of(1, ChronoUnit.MINUTES)), makeHist(curTime.minusSeconds(30))
            )
        );
        assertEquals(
            new BigDecimal("0"),
            strat.calOverdue(
                curTime, strat.durToBigDec(Duration.of(1, ChronoUnit.MINUTES)), makeHist(curTime.minusSeconds(60))
            )
        );
        assertEquals(
            new BigDecimal("0"),
            strat.calOverdue(
                curTime, strat.durToBigDec(Duration.of(1, ChronoUnit.MINUTES)), List.of()
            )
        );
        assertEquals(
            new BigDecimal("0.1"),
            strat.calOverdue(
                curTime, strat.durToBigDec(Duration.of(1, ChronoUnit.MINUTES)), makeHist(curTime.minusSeconds(66))
            )
        );
        assertEquals(
            new BigDecimal("0.2"),
            strat.calOverdue(
                curTime, strat.durToBigDec(Duration.of(1, ChronoUnit.MINUTES)), makeHist(curTime.minusSeconds(72))
            )
        );
        assertEquals(
            new BigDecimal("0.5"),
            strat.calOverdue(
                curTime, strat.durToBigDec(Duration.of(1, ChronoUnit.MINUTES)), makeHist(curTime.minusSeconds(90))
            )
        );
        assertEquals(
            new BigDecimal("1"),
            strat.calOverdue(
                curTime, strat.durToBigDec(Duration.of(1, ChronoUnit.MINUTES)), makeHist(curTime.minusSeconds(120))
            )
        );
        assertEquals(
            new BigDecimal("2"),
            strat.calOverdue(
                curTime, strat.durToBigDec(Duration.of(1, ChronoUnit.MINUTES)), makeHist(curTime.minusSeconds(180))
            )
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
        return List.of(org.igye.remem3.app.dto.HistRec.builder().time(time).build());
    }

}