package org.igye.remem3.app.repeatstrategy.impl;

import lombok.Builder;
import lombok.Getter;
import org.apache.commons.lang3.tuple.Pair;
import org.igye.remem3.app.dto.RepeatStrategyType;
import org.igye.remem3.app.repeatstrategy.Task;
import org.igye.remem3.html.HtmlElem;
import org.igye.remem3.utils.Utils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static java.lang.String.format;

public class RepeatStrategyRetryFailed extends BaseRepeatStrategy {

    private final Utils utils;
    private final double randomnessFactor;
    private int round;
    private RepeatStrategyCircle circle;

    public RepeatStrategyRetryFailed(
        Utils utils,
        List<Task> allTasks,
        Instant startTime,
        double randomnessFactor
    ) {
        super(allTasks, startTime);
        this.utils = utils;
        this.randomnessFactor = randomnessFactor;
        this.round = 1;
        this.circle = new RepeatStrategyCircle(
            utils, getNotPassedTasks(), startTime, randomnessFactor, Optional.of(1)
        );
    }

    @Override
    public Optional<List<Task>> getNextTasks() {
        if (getAllTasks().isEmpty()) {
            return Optional.empty();
        }
        Optional<List<Task>> nextTasks = circle.getNextTasks();
        if (nextTasks.isPresent()) {
            return nextTasks;
        }
        List<Task> notPassedTasks = getNotPassedTasks();
        if (notPassedTasks.isEmpty()) {
            return Optional.empty();
        }
        this.round++;
        this.circle = new RepeatStrategyCircle(
            utils, notPassedTasks, startTime, randomnessFactor, Optional.of(2)
        );
        return circle.getNextTasks();
    }

    @Override
    public HtmlElem renderLessParams(boolean historyUpdated) {
        RoundStats roundStats = getRoundStats();
        return frag(
            text(String.format("%s: ", RepeatStrategyType.RETRY_FAILED)),
            text(format("Round: %s", round)),
            text(format("Left: %s Failed: %s", roundStats.getToBeAsked(), roundStats.getFailed()))
        );
    }

    @Override
    public HtmlElem renderMoreParams(boolean historyUpdated) {
        RoundStats roundStats = getRoundStats();
        return frag(
            div(text(format("Repeat strategy: %s", RepeatStrategyType.RETRY_FAILED))),
            div(text(format("Directories: %s", getDirectoriesStr()))),
            div(text(format("Task types: %s", getTaskTypesStr()))),
            div(text(format("Number of tasks: %s", getAllTasks().size()))),
            div(text(format("Randomness: %s", randomnessFactor))),
            div(text(format("Round: %s", round))),
            div(text(format("Left: %s Failed: %s", roundStats.getToBeAsked(), roundStats.getFailed()))),
            div(text(format("Start time: %s", startTime.truncatedTo(ChronoUnit.SECONDS)))),
            div(text(format(
                "Min. history time: %s",
                minHistTime.map(inst -> inst.truncatedTo(ChronoUnit.SECONDS)).map(Instant::toString).orElse("-")
            )))
        );
    }

    @Override
    public boolean hasDailyUniqueCount() {
        return false;
    }

    @Override
    public Optional<Pair<Long, Long>> getDailyUniqueCount() {
        return Optional.empty();
    }

    private List<Task> getNotPassedTasks() {
        return getAllTasks().stream()
            .filter(task -> task.getHist().getRecords().isEmpty() || !task.getHist().getRecords().getLast().isPassed())
            .map(this::truncateHist)
            .toList();
    }

    private Task truncateHist(Task task) {
        TaskImpl taskImpl = (TaskImpl) task;
        return new TaskImpl(
            taskImpl.getBaseTask(),
            //we need to preserve the last history record so the Circle strategy shows tasks in consistent order
            task.getHist().getRecords().isEmpty()
                ? Instant.now()
                : task.getHist().getRecords().getLast().getTime(),
            task.getSelectedByStrategyType()
        );
    }

    private RoundStats getRoundStats() {
        List<Task> notPassedTasks = getNotPassedTasks();
        long toBeAsked = notPassedTasks.stream().filter(t ->
                t.getHist().getRecords().isEmpty()
                    || round > 1 && t.getHist().getRecords().size() == 1
            )
            .count();
        return RoundStats.builder()
            .passed(getAllTasks().size() - notPassedTasks.size())
            .toBeAsked(toBeAsked)
            .failed(notPassedTasks.size() - toBeAsked)
            .build();
    }

    @Builder
    @Getter
    private static class RoundStats {
        private final long failed;
        private final long passed;
        private final long toBeAsked;
    }
}
