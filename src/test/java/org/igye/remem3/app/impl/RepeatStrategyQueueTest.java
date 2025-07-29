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

    @Test
    void compareTasks() {
        RepeatStrategyQueue strat = new RepeatStrategyQueue(
            new UtilsImpl(new ObjectMapper()), 3, 3, List.of()
        );
        //both are new tasks
        assertCorrectOrder(strat.compare(makeTask(0, 0), makeTask(0, 0)));
        assertCorrectOrder(strat.compare(makeTask(0, 1), makeTask(0, 0)));
        assertCorrectOrder(strat.compare(makeTask(0, 0), makeTask(0, 1)));

        //one task is new another is not
        assertCorrectOrder(strat.compare(makeTask(1, 0), makeTask(0, 0)));
        assertIncorrectOrder(strat.compare(makeTask(0, 0), makeTask(1, 0)));

        assertCorrectOrder(strat.compare(makeTask(1, 0), makeTask(0, 1)));
        assertIncorrectOrder(strat.compare(makeTask(0, 1), makeTask(1, 0)));

        assertCorrectOrder(strat.compare(makeTask(1, 1), makeTask(0, 0)));
        assertIncorrectOrder(strat.compare(makeTask(0, 0), makeTask(1, 1)));

        //both tasks are old
        assertCorrectOrder(strat.compare(makeTask(1, 0), makeTask(1, 0)));
        assertCorrectOrder(strat.compare(makeTask(1, 0), makeTask(2, 0)));
        assertCorrectOrder(strat.compare(makeTask(2, 0), makeTask(1, 0)));

        assertCorrectOrder(strat.compare(makeTask(1, 0), makeTask(1, 1)));
        assertCorrectOrder(strat.compare(makeTask(1, 0), makeTask(2, 1)));
        assertCorrectOrder(strat.compare(makeTask(2, 0), makeTask(1, 1)));

        assertIncorrectOrder(strat.compare(makeTask(1, 1), makeTask(1, 0)));
        assertIncorrectOrder(strat.compare(makeTask(2, 1), makeTask(1, 0)));
        assertIncorrectOrder(strat.compare(makeTask(1, 1), makeTask(2, 0)));
    }

    private RepeatStrategyQueue.TaskDto makeTask(int histLen, int streak) {
        return RepeatStrategyQueue.TaskDto.builder().histLen(histLen).streak(streak).build();
    }

    private void assertCorrectOrder(int cmpRes) {
        Assertions.assertTrue(cmpRes == -1 || cmpRes == 0);
    }

    private void assertIncorrectOrder(int cmpRes) {
        Assertions.assertEquals(1, cmpRes);
    }

    private List<HistRec> makeHist(BigDecimal... marks) {
        return Arrays.stream(marks).map(mark -> HistRec.builder().mark(mark).build()).toList();
    }
}