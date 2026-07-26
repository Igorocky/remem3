package org.igye.remem3.app.repeatstrategy.impl;

import org.apache.commons.lang3.tuple.Pair;
import org.igye.remem3.app.dto.RepeatStrategyType;
import org.igye.remem3.app.repeatstrategy.Hist;
import org.igye.remem3.app.repeatstrategy.HistRec;
import org.igye.remem3.app.repeatstrategy.Task;
import org.igye.remem3.html.HtmlElem;
import org.igye.remem3.utils.Utils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static java.lang.String.format;

public class RepeatStrategyRetryFailed extends BaseRepeatStrategy {

    private final Utils utils;
    private final double randomnessFactor;
    private final Instant startTime;
    private final boolean keepOrder;
    private int round;
    private RepeatStrategyCircle circle;

    public RepeatStrategyRetryFailed(
        Utils utils,
        List<Task> allTasks,
        double randomnessFactor,
        boolean keepOrder
    ) {
        super(allTasks);
        this.utils = utils;
        this.randomnessFactor = randomnessFactor;
        this.startTime = allTasks.stream()
            .map(Task::getHist)
            .map(Hist::getRecords)
            .flatMap(Collection::stream)
            .map(HistRec::getTime)
            .min(Instant::compareTo)
            .orElseGet(Instant::now);
        this.keepOrder = keepOrder;
        this.round = 1;
        this.circle = new RepeatStrategyCircle(
            utils, getNotPassedTasks(true), randomnessFactor, Optional.of(1), keepOrder
        );
    }

    @Override
    public Optional<List<Task>> getNextTasks() {
        if (getAllTasks().isEmpty()) {
            return Optional.empty();
        }
        Optional<List<Task>> nextTasks = circle.getNextTasks();
        if (nextTasks.isEmpty()) {
            List<Task> notPassedTasks = getNotPassedTasks(true);
            if (notPassedTasks.isEmpty()) {
                return Optional.empty();
            } else {
                this.round++;
                this.circle = new RepeatStrategyCircle(
                    utils, notPassedTasks, randomnessFactor, Optional.of(2), keepOrder
                );
                return circle.getNextTasks();
            }
        } else {
            return nextTasks;
        }
    }

    @Override
    public HtmlElem renderLessParams(boolean historyUpdated) {
        return frag(
            text(String.format("%s: ", RepeatStrategyType.RETRY_FAILED)),
            text(format("Round: %s", round)),
            text(format("Passed: %s/%s",
                getAllTasks().size() - getNotPassedTasks(false).size(), getAllTasks().size()
            ))
        );
    }

    @Override
    public HtmlElem renderMoreParams(boolean historyUpdated) {
        return frag(
            div(text(format("Repeat strategy: %s", RepeatStrategyType.RETRY_FAILED))),
            div(text(format("Directories: %s", getDirectoriesStr()))),
            div(text(format("Task types: %s", getTaskTypesStr()))),
            div(text(format("Number of tasks: %s", getAllTasks().size()))),
            div(text(format("Keep order: %s", keepOrder))),
            div(text(format("Randomness: %s", randomnessFactor))),
            div(text(format("Round: %s", round))),
            div(text(format("Passed: %s/%s",
                getAllTasks().size() - getNotPassedTasks(false).size(), getAllTasks().size()
            ))),
            div(text(format("Start time: %s", startTime.truncatedTo(ChronoUnit.SECONDS))))
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

    private List<Task> getNotPassedTasks(boolean preserveLastHistRec) {
        return getAllTasks().stream()
            .filter(task -> task.getHist().getRecords().isEmpty() || !task.getHist().getRecords().getLast().isPassed())
            .map(task -> truncateHist(task, preserveLastHistRec))
            .toList();
    }

    private Task truncateHist(Task task, boolean preserveLastRec) {
        TaskImpl taskImpl = (TaskImpl) task;
        return new TaskImpl(
            taskImpl.getBaseTask(),
            //we need to preserve the last history record so the Circle strategy shows tasks in consistent order
            (task.getHist().getRecords().isEmpty() || !preserveLastRec)
                ? Instant.now()
                : task.getHist().getRecords().getLast().getTime(),
            task.getSelectedByStrategyType()
        );
    }

}
