package org.igye.remem3.app.repeatstrategy.impl;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.igye.remem3.app.dto.RepeatStrategyType;
import org.igye.remem3.app.repeatstrategy.HistRec;
import org.igye.remem3.app.repeatstrategy.RepeatStrategy;
import org.igye.remem3.app.repeatstrategy.Task;
import org.igye.remem3.utils.impl.UtilsImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

class RepeatStrategyCompoundTest {
    private final UtilsImpl utils = new UtilsImpl(new ObjectMapper());

    @Test
    void getNextTasks_returns_empty_if_all_strategies_return_empty() {
        //given
        RepeatStrategy str1 = new RepeatStrategyCircle(
            utils, makeTasks(5, "1"), 0.3, Optional.of(1), false
        );
        RepeatStrategy str2 = new RepeatStrategyCircle(
            utils, makeTasks(2, "2"), 0.3, Optional.of(1), false
        );
        RepeatStrategy str3 = new RepeatStrategyCircle(
            utils, makeTasks(3, "3"), 0.3, Optional.of(1), false
        );
        RepeatStrategyCompound compound = new RepeatStrategyCompound(List.of(
            Pair.of(1, str1), Pair.of(1, str2), Pair.of(1, str3)
        ));
        int maxCnt = 100;
        int cnt = 0;

        //when
        Optional<List<Task>> nextTasksOpt = compound.getNextTasks();
        while (nextTasksOpt.isPresent() && cnt < maxCnt) {
            List<Task> nextTasks = nextTasksOpt.get();
            cnt += nextTasks.size();
            nextTasks.forEach(t -> t.getHist().add(makeHistRec()));
            nextTasksOpt = compound.getNextTasks();
        }

        //then
        Assertions.assertEquals(10, cnt);
    }

    @Test
    void getNextTasks_returns_tasks_with_specified_probability() {
        //given
        RepeatStrategy str1 = new RepeatStrategyCircle(
            utils, makeTasks(5, "1"), 0.3, Optional.empty(), false
        );
        RepeatStrategy str2 = new RepeatStrategyCircle(
            utils, makeTasks(2, "2"), 0.3, Optional.empty(), false
        );
        RepeatStrategy str3 = new RepeatStrategyCircle(
            utils, makeTasks(3, "3"), 0.3, Optional.empty(), false
        );
        RepeatStrategyCompound compound = new RepeatStrategyCompound(List.of(
            Pair.of(10, str1), Pair.of(30, str2), Pair.of(60, str3)
        ));
        int maxCnt = 10_000;
        int cnt = 0;
        Map<String, AtomicInteger> counts = new HashMap<>();

        //when
        Optional<List<Task>> nextTasksOpt = compound.getNextTasks();
        while (nextTasksOpt.isPresent() && cnt < maxCnt) {
            List<Task> nextTasks = nextTasksOpt.get();
            cnt += nextTasks.size();
            nextTasks.forEach(task -> {
                task.getHist().add(makeHistRec());
                counts.computeIfAbsent(((TestTask) task).getContent(), _ -> new AtomicInteger(0)).incrementAndGet();
            });
            nextTasksOpt = compound.getNextTasks();
        }

        //then
        assertCnt(maxCnt, 0.1, counts.get("1").get());
        assertCnt(maxCnt, 0.3, counts.get("2").get());
        assertCnt(maxCnt, 0.6, counts.get("3").get());
    }

    private void assertCnt(int maxCnt, double pct, int actualCnt) {
        double expCnt = maxCnt * pct;
        double minExpCnt = expCnt * 0.9;
        double maxExpCnt = expCnt * 1.1;
        Assertions.assertTrue(minExpCnt <= actualCnt);
        Assertions.assertTrue(actualCnt <= maxExpCnt);
    }

    private List<Task> makeTasks(int count, String content) {
        return Stream.generate(() -> content).limit(count).map(_ -> makeTask(content)).toList();
    }

    private Task makeTask(String content) {
        return new TestTask(content);
    }

    private HistRec makeHistRec() {
        return new TestHistRec(Instant.now(), BigDecimal.ONE);
    }

    private static class TestTask implements Task {
        private final List<HistRec> hist = new ArrayList<>();
        @Getter
        private final String content;

        private TestTask(String content) {
            this.content = content;
        }

        @Override
        public List<HistRec> getHist() {
            return hist;
        }

        @Override
        public String getDir() {
            return "";
        }

        @Override
        public RepeatStrategyType getSelectedByStrategyType() {
            return RepeatStrategyType.CIRCLE;
        }
    }

    @RequiredArgsConstructor
    @Getter
    private static class TestHistRec implements HistRec {
        private final Instant time;
        private final BigDecimal mark;
    }
}