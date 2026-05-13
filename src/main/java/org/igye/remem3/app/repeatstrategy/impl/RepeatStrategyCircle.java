package org.igye.remem3.app.repeatstrategy.impl;

import lombok.Builder;
import lombok.Getter;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class RepeatStrategyCircle extends HtmlBuilder implements RepeatStrategy {

    public static final int MAX_NUM_OF_ROUNDS = 1_000_000;
    public static final BigDecimal DEFAULT_RND_FACTOR = new BigDecimal("0.3");

    private final List<Task> allTasks;
    private final Instant startTime;
    private final double randomnessFactor;
    private final Optional<Integer> numOfRounds;
    private final boolean keepOrder;
    private final Random rnd;

    public RepeatStrategyCircle(
        Utils utils,
        List<Task> allTasks,
        double randomnessFactor,
        Optional<Integer> numOfRounds,
        boolean keepOrder
    ) {
        if (CollectionUtils.isEmpty(allTasks)) {
            throw new Exn("There are no tasks.");
        }
        this.allTasks = Collections.unmodifiableList(allTasks);
        this.startTime = allTasks.stream()
            .map(Task::getHist)
            .flatMap(Collection::stream)
            .map(HistRec::getTime)
            .min(Instant::compareTo)
            .orElseGet(Instant::now);
        this.randomnessFactor = utils.getInRange(0, randomnessFactor, 1);
        this.numOfRounds = numOfRounds.map(n -> Math.max(1, Math.min(n, MAX_NUM_OF_ROUNDS)));
        this.keepOrder = keepOrder;
        rnd = new Random();
    }

    @Override
    public Optional<List<Task>> getNextTasks() {
        Stats stats = getStats();
        List<Task> tasksToSelectFrom = stats.getTasksWithMinHistLen().stream()
            .sorted(Comparator.comparing(TaskDto::getLastTime))
            .limit(stats.getNumOfTasksToSelectFrom())
            .map(TaskDto::getTask)
            .toList();
        if (tasksToSelectFrom.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(List.of(tasksToSelectFrom.get(rnd.nextInt(tasksToSelectFrom.size()))));
        }
    }

    @Override
    public HtmlElem renderLessParams(boolean historyUpdated) {
        ProgressInfo progressInfo = calcRoundProgress(historyUpdated, getStats());
        return frag(
            text(String.format("%s: ", RepeatStrategyType.CIRCLE)),
            numOfRounds.isPresent()
                ? text(format("Round: %s/%s", progressInfo.getRound(), numOfRounds.get()))
                : text(format("Round: %s", progressInfo.getRound())),
            text(format("Round progress: %s/%s", progressInfo.getRoundProgress(), allTasks.size()))
        );
    }

    @Override
    public HtmlElem renderMoreParams(boolean historyUpdated) {
        Stats stats = getStats();
        ProgressInfo progressInfo = calcRoundProgress(historyUpdated, stats);
        long numOfTasksToSelectFrom = stats.getNumOfTasksToSelectFrom();
        String tasksStr = numOfTasksToSelectFrom == 1 ? "task" : "tasks";
        return frag(
            div(text(format("Number of tasks: %s", allTasks.size()))),
            div(text(format("Keep order: %s", keepOrder))),
            div(text(format("Randomness: %s (%s %s)", randomnessFactor, numOfTasksToSelectFrom, tasksStr))),
            numOfRounds.isPresent()
                ? div(text(format("Round: %s/%s", progressInfo.getRound(), numOfRounds.get())))
                : div(text(format("Round: %s", progressInfo.getRound()))),
            div(text(format("Round progress: %s/%s", progressInfo.getRoundProgress(), allTasks.size()))),
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

    private ProgressInfo calcRoundProgress(boolean historyUpdated, Stats stats) {
        int minHistLen = stats.getMinHistLen();
        int numOfTasksWithMinHistLen = stats.getTasksWithMinHistLen().size();
        //numOfTasksWithMinHistLen == 0 this will be at the end of the last round
        int round = historyUpdated && (numOfTasksWithMinHistLen == allTasks.size() || numOfTasksWithMinHistLen == 0)
            ? minHistLen
            : minHistLen + 1;
        int roundProgress;
        if (numOfTasksWithMinHistLen == allTasks.size()) {
            roundProgress = historyUpdated ? allTasks.size() : 1;
        } else {
            roundProgress = allTasks.size() - numOfTasksWithMinHistLen + (historyUpdated ? 0 : 1);
        }
        return ProgressInfo.builder()
            .round(round)
            .roundProgress(roundProgress)
            .build();
    }

    private Stats getStats() {
        List<TaskDto> tasks = getTaskDtos();
        int minHistLen = tasks.isEmpty()
            ? 0
            : tasks.stream().map(TaskDto::getHist).map(List::size).reduce(Integer.MAX_VALUE, Math::min);
        return Stats.builder()
            .tasks(tasks)
            .minHistLen(minHistLen)
            .tasksWithMinHistLen(
                tasks.stream()
                    .filter(task -> {
                        int histLen = task.getHist().size();
                        return numOfRounds.map(nr -> histLen < nr).orElse(true) && histLen == minHistLen;
                    })
                    .toList()
            )
            .numOfTasksToSelectFrom(Math.max(1L, Math.round(allTasks.size() * randomnessFactor)))
            .build();
    }

    private List<TaskDto> getTaskDtos() {
        ArrayList<TaskDto> res = allTasks.stream()
            .map(task -> {
                List<HistRec> hist = task.getHist();
                return TaskDto.builder()
                    .task(task)
                    .hist(hist)
                    .lastTime(hist.isEmpty() ? startTime : hist.getLast().getTime())
                    .build();
            })
            .collect(Collectors.toCollection(ArrayList::new));
        if (!keepOrder) {
            Collections.shuffle(res);
        }
        return res;
    }

    @Getter
    @Builder
    private static class Stats {
        private List<TaskDto> tasks;
        private int minHistLen;
        private List<TaskDto> tasksWithMinHistLen;
        private long numOfTasksToSelectFrom;
    }

    @Builder
    @Getter
    private static class TaskDto {
        private Task task;
        private List<HistRec> hist;
        private Instant lastTime;
    }

    @Builder
    @Getter
    private static class ProgressInfo {
        private int round;
        private int roundProgress;
    }
}
