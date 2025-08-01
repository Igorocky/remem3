package org.igye.remem3.app.repeatstrategy.impl;

import lombok.Builder;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.igye.remem3.app.repeatstrategy.HistRec;
import org.igye.remem3.app.repeatstrategy.RepeatStrategy;
import org.igye.remem3.app.repeatstrategy.Task;
import org.igye.remem3.html.HtmlBuilder;
import org.igye.remem3.html.HtmlElem;
import org.igye.remem3.utils.Exn;
import org.igye.remem3.utils.Utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class RepeatStrategyBuckets extends HtmlBuilder implements RepeatStrategy {

    public static final int MIN_BATCH_SIZE = 1;
    public static final int MAX_BATCH_SIZE = 10;
    public static final int DEFAULT_BATCH_SIZE = 5;
    private final Utils utils;
    private final Clock clock;
    private final int batchSize;
    private final List<Task> allTasks;
    private final List<Duration> bucketDelays;
    private final List<BigDecimal> bucketDelaysBigDec;
    private final int maxBucketNum;

    public RepeatStrategyBuckets(
        Utils utils,
        Clock clock,
        int batchSize,
        List<Task> allTasks,
        List<Duration> bucketDelays
    ) {
        if (CollectionUtils.isEmpty(bucketDelays)) {
            throw new Exn("At least one bucket must be defined.");
        }
        this.utils = utils;
        this.clock = clock;
        this.batchSize = utils.getInRange(MIN_BATCH_SIZE, batchSize, MAX_BATCH_SIZE);
        this.allTasks = Collections.unmodifiableList(allTasks);
        this.bucketDelays = Collections.unmodifiableList(bucketDelays);
        this.bucketDelaysBigDec = bucketDelays.stream().map(this::durToBigDec).toList();
        this.maxBucketNum = bucketDelays.size() - 1;
    }

    @Override
    public Optional<List<Task>> getNextTasks() {
        List<TaskDto> allTasks = getTaskDtos();
        ArrayList<TaskDto> activeTasks = getPreferredActiveTasks(allTasks);
        ArrayList<String> preferredDirs = getPreferredDirs(activeTasks);
        List<TaskDto> selectedTasks = selectActiveTasksToRepeat(activeTasks, preferredDirs, batchSize);
        Collections.shuffle(selectedTasks);
        return Optional.of(selectedTasks.stream().map(TaskDto::getTask).toList());
    }

    @Override
    public HtmlElem renderParams(boolean historyUpdated) {
        ArrayList<List<? extends HtmlElem>> rows = new ArrayList<>();
        List<HtmlElem> headerRow = new ArrayList<>();
        List<HtmlElem> delayRow = new ArrayList<>();
        List<HtmlElem> activeRow = new ArrayList<>();
        List<HtmlElem> waitingRow = new ArrayList<>();
        List<HtmlElem> totalRow = new ArrayList<>();
        rows.add(headerRow);
        rows.add(delayRow);
        rows.add(activeRow);
        rows.add(waitingRow);
        rows.add(totalRow);
        headerRow.add(text("Bucket number"));
        delayRow.add(text("Bucket delay"));
        activeRow.add(text("Active"));
        waitingRow.add(text("Waiting"));
        totalRow.add(text("Total"));
        List<Pair<ArrayList<TaskDto>, ArrayList<TaskDto>>> buckets = bucketDelays.stream()
            .map(_ -> Pair.of(new ArrayList<TaskDto>(), new ArrayList<TaskDto>()))
            .toList();
        for (TaskDto task : getTaskDtos()) {
            Pair<ArrayList<TaskDto>, ArrayList<TaskDto>> bucket = buckets.get(task.getBucketNum());
            if (task.isActive()) {
                bucket.getRight().add(task);
            } else {
                bucket.getLeft().add(task);
            }
        }
        for (int b = 0; b < buckets.size(); b++) {
            headerRow.add(text(String.valueOf(b + 1)));
            Duration bucketDelay = bucketDelays.get(b);
            delayRow.add(text(utils.durationToStr(bucketDelay)));
            int activeCnt = buckets.get(b).getRight().size();
            List<TaskDto> waitingTasks = buckets.get(b).getLeft();
            int waitingCnt = waitingTasks.size();
            activeRow.add(text(String.valueOf(activeCnt)));
            if (activeCnt == 0 && waitingCnt > 0) {
                Duration timeToWait = waitingTasks.stream()
                    .map(TaskDto::getOverdue)
                    .max(BigDecimal::compareTo)
                    .map(BigDecimal::negate)
                    .map(durToBigDec(bucketDelay)::multiply)
                    .map(BigDecimal::longValue)
                    .map(Duration::ofSeconds)
                    .get();
                waitingRow.add(text(format("%s (%s)", waitingCnt, getApproxDurationStr(timeToWait))));
            } else {
                waitingRow.add(text(String.valueOf(waitingCnt)));
            }
            totalRow.add(text(String.valueOf(activeCnt + waitingCnt)));
        }
        return frag(
            div(text(format("Number of tasks: %s", allTasks.size()))),
            div(text(format("Batch size: %s", batchSize))),
            div(table(rows).attr("class", "table-single-border bucket-params"))
        );
    }

    private ArrayList<TaskDto> selectActiveTasksToRepeat(
        ArrayList<TaskDto> activeTasks,
        ArrayList<String> preferredDirs,
        int maxNumOfTasksToSelect
    ) {
        ArrayList<TaskDto> selectedTasks = new ArrayList<>();
        mainLoop:
        while (selectedTasks.size() < maxNumOfTasksToSelect && !activeTasks.isEmpty()) {
            String curDir = preferredDirs.getFirst();
            for (int i = 0; i < activeTasks.size(); i++) {
                TaskDto task = activeTasks.get(i);
                if (curDir.equals(task.getDir())) {
                    selectedTasks.add(task);
                    activeTasks.remove(i);
                    preferredDirs.removeFirst();
                    if (activeTasks.stream().anyMatch(t -> curDir.equals(t.getDir()))) {
                        preferredDirs.add(curDir);
                    }
                    continue mainLoop;
                }
            }
            throw new Exn(format("Cannot find an active task in the directory %s", curDir));
        }
        return selectedTasks;
    }

    private ArrayList<TaskDto> getPreferredActiveTasks(List<TaskDto> allTasks) {
        return allTasks.stream()
            .filter(TaskDto::isActive)
            .sorted(Comparator.comparing(TaskDto::getOverdue).reversed())
            .collect(Collectors.toCollection(ArrayList::new));
    }

    private ArrayList<String> getPreferredDirs(List<TaskDto> activeTasks) {
        Map<String, Instant> dirToLastTime = activeTasks.stream()
            .collect(Collectors.toMap(
                TaskDto::getDir,
                TaskDto::getLastTime,
                (t1, t2) -> t1.compareTo(t2) < 0 ? t2 : t1
            ));
        return dirToLastTime.entrySet().stream()
            .sorted(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .collect(Collectors.toCollection(ArrayList::new));
    }

    private String getApproxDurationStr(Duration dur) {
        return Arrays.stream(utils.durationToStr(dur).split("\\s+"))
            .filter(StringUtils::isNotBlank)
            .findFirst()
            .get();
    }

    protected BigDecimal durToBigDec(Duration dur) {
        return BigDecimal.valueOf(dur.getSeconds()).setScale(10, RoundingMode.HALF_UP);
    }

    protected BigDecimal calOverdue(Instant curTime, BigDecimal bucketDelay, List<HistRec> hist) {
        if (hist.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal taskDelay = durToBigDec(Duration.between(hist.getLast().getTime(), curTime));
        return utils.calOverdue(bucketDelay, taskDelay);
    }

    private int getBucketNum(List<HistRec> hist) {
        return utils.getInRange(0, utils.getStreak(hist), maxBucketNum);
    }

    private List<TaskDto> getTaskDtos() {
        Instant curTime = clock.instant();
        return allTasks.stream()
            .map(task -> {
                List<HistRec> hist = task.getHist();
                int bucketNum = getBucketNum(hist);
                BigDecimal overdue = calOverdue(curTime, bucketDelaysBigDec.get(bucketNum), hist);
                return TaskDto.builder()
                    .task(task)
                    .dir(task.getDir())
                    .lastTime(hist.isEmpty() ? Instant.MIN : hist.getLast().getTime())
                    .bucketNum(bucketNum)
                    .overdue(overdue)
                    .active(BigDecimal.ZERO.compareTo(overdue) <= 0)
                    .build();
            })
            .toList();
    }

    @Builder
    @Getter
    private static class TaskDto {
        private Task task;
        private String dir;
        private Instant lastTime;
        private int bucketNum;
        private BigDecimal overdue;
        private boolean active;
    }
}
