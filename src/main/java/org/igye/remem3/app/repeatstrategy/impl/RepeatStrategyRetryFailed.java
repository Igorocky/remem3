package org.igye.remem3.app.repeatstrategy.impl;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.igye.remem3.app.dto.RepeatStrategyType;
import org.igye.remem3.app.repeatstrategy.HistRec;
import org.igye.remem3.app.repeatstrategy.RepeatStrategy;
import org.igye.remem3.app.repeatstrategy.Task;
import org.igye.remem3.html.HtmlBuilder;
import org.igye.remem3.html.HtmlElem;
import org.igye.remem3.utils.Exn;
import org.igye.remem3.utils.Utils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static java.lang.String.format;

public class RepeatStrategyRetryFailed extends HtmlBuilder implements RepeatStrategy {

    private final Utils utils;
    private final double randomnessFactor;
    private final List<Task> allTasks;
    private final Instant startTime;
    private int round;
    private RepeatStrategyCircle circle;

    public RepeatStrategyRetryFailed(
        Utils utils,
        List<Task> allTasks,
        double randomnessFactor
    ) {
        if (CollectionUtils.isEmpty(allTasks)) {
            throw new Exn("There are no tasks.");
        }
        this.utils = utils;
        this.randomnessFactor = randomnessFactor;
        this.allTasks = Collections.unmodifiableList(allTasks);
        this.startTime = allTasks.stream()
            .map(Task::getHist)
            .flatMap(Collection::stream)
            .map(HistRec::getTime)
            .min(Instant::compareTo)
            .orElseGet(Instant::now);
        this.round = 1;
        this.circle = new RepeatStrategyCircle(
            utils, getNotPassedTasks(), randomnessFactor, Optional.of(1)
        );
    }

    @Override
    public Optional<List<Task>> getNextTasks() {
        Optional<List<Task>> nextTasks = circle.getNextTasks();
        if (nextTasks.isEmpty()) {
            List<Task> notPassedTasks = getNotPassedTasks();
            if (notPassedTasks.isEmpty()) {
                return Optional.empty();
            } else {
                this.round++;
                //todo: tweak task history and pass Optional.of(1)
                this.circle = new RepeatStrategyCircle(
                    utils, notPassedTasks, randomnessFactor, Optional.of(round)
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
            text(format("Passed: %s/%s", allTasks.size() - getNotPassedTasks().size(), allTasks.size()))
        );
    }

    @Override
    public HtmlElem renderMoreParams(boolean historyUpdated) {
        return frag(
            div(text(format("Number of tasks: %s", allTasks.size()))),
            div(text(format("Randomness: %s", randomnessFactor))),
            div(text(format("Round: %s", round))),
            div(text(format("Passed: %s/%s", allTasks.size() - getNotPassedTasks().size(), allTasks.size()))),
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

    private List<Task> getNotPassedTasks() {
        return allTasks.stream()
            .filter(task -> task.getHist().isEmpty() || !task.getHist().getLast().isPassed())
            .toList();
    }

}
