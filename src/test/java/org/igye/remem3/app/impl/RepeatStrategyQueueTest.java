package org.igye.remem3.app.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.igye.remem3.app.dto.HistRec;
import org.igye.remem3.utils.impl.UtilsImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

class RepeatStrategyQueueTest {

    @Test
    void countStreak() {
        RepeatStrategyQueue strat = new RepeatStrategyQueue(
            new UtilsImpl(new ObjectMapper()), 3, 7, List.of()
        );
        Assertions.assertEquals(0, strat.countStreak(List.of()));
        Assertions.assertEquals(
            0,
            strat.countStreak(makeHist(BigDecimal.ZERO))
        );
        Assertions.assertEquals(
            0,
            strat.countStreak(makeHist(BigDecimal.ZERO, BigDecimal.ZERO))
        );
        Assertions.assertEquals(
            0,
            strat.countStreak(makeHist(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO))
        );
        Assertions.assertEquals(
            0,
            strat.countStreak(makeHist(BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO))
        );
        Assertions.assertEquals(
            0,
            strat.countStreak(makeHist(BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ZERO))
        );
        Assertions.assertEquals(
            0,
            strat.countStreak(makeHist(BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.ZERO))
        );
        Assertions.assertEquals(
            1,
            strat.countStreak(makeHist(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ONE))
        );
        Assertions.assertEquals(
            2,
            strat.countStreak(makeHist(BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.ONE))
        );
        Assertions.assertEquals(
            3,
            strat.countStreak(makeHist(BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE))
        );
        Assertions.assertEquals(
            2,
            strat.countStreak(makeHist(BigDecimal.ONE, BigDecimal.ONE))
        );
        Assertions.assertEquals(
            1,
            strat.countStreak(makeHist(BigDecimal.ONE))
        );
    }

    private List<HistRec> makeHist(BigDecimal... marks) {
        return Arrays.stream(marks).map(mark -> HistRec.builder().mark(mark).build()).toList();
    }
}