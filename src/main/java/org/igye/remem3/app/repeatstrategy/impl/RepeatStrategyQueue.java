package org.igye.remem3.app.repeatstrategy.impl;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.tuple.Pair;
import org.igye.remem3.app.dto.RepeatStrategyType;
import org.igye.remem3.app.repeatstrategy.HistRec;
import org.igye.remem3.app.repeatstrategy.Task;
import org.igye.remem3.html.HtmlElem;
import org.igye.remem3.utils.Exn;
import org.igye.remem3.utils.Utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static java.lang.String.format;

public class RepeatStrategyQueue extends BaseRepeatStrategy {

    public static final int MIN_STEP = 1;
    public static final int MAX_STEP = 20;
    public static final BigDecimal MIN_STEP_MULT_FACTOR = BigDecimal.ONE;
    public static final BigDecimal MAX_STEP_MULT_FACTOR = new BigDecimal("100");
    public static final int MIN_BATCH_SIZE = 1;
    public static final int MAX_BATCH_SIZE = 10;

    private final Utils utils;
    private final int step;
    private final BigDecimal stepMultFactor;
    private final int batchSize;
    private final int maxHistLenStat;

    private final List<Integer> bucketDelays;
    private final int maxBucketIdx;
    private final Instant startTimeForParams;

    public RepeatStrategyQueue(
        Utils utils, List<Task> allTasks, int step, BigDecimal stepMultFactor, int batchSize, int maxHistLenStat
    ) {
        super(allTasks);
        this.utils = utils;
        this.step = utils.getInRange(MIN_STEP, step, MAX_STEP);
        this.stepMultFactor = utils.getInRange(MIN_STEP_MULT_FACTOR, stepMultFactor, MAX_STEP_MULT_FACTOR);
        this.batchSize = utils.getInRange(MIN_BATCH_SIZE, batchSize, MAX_BATCH_SIZE);
        this.maxHistLenStat = utils.getInRange(2, maxHistLenStat, 30);
        List<Integer> bucketDelays = new ArrayList<>();
        int maxDelay = utils.getInRange(0, allTasks.size() - batchSize, allTasks.size());
        do {
            if (bucketDelays.isEmpty()) {
                bucketDelays.add(0);
            } else if (bucketDelays.size() == 1) {
                bucketDelays.add(step);
            } else {
                bucketDelays.add(
                    stepMultFactor.multiply(BigDecimal.valueOf(bucketDelays.getLast()))
                        .setScale(0, RoundingMode.CEILING).intValue()
                );
            }
        } while (bucketDelays.getLast() < maxDelay);
        bucketDelays.set(bucketDelays.size() - 1, maxDelay);
        this.bucketDelays = Collections.unmodifiableList(bucketDelays);
        this.maxBucketIdx = bucketDelays.size() - 1;
        this.startTimeForParams = Instant.now();
    }

    @Override
    public Optional<List<Task>> getNextTasks() {
        if (getAllTasks().isEmpty()) {
            return Optional.empty();
        }
        List<TaskDto> allTasks = new ArrayList<>(getTaskDtos());
        Collections.shuffle(allTasks);
        ArrayList<Task> nextTasks = new ArrayList<>(
            allTasks.stream()
                .filter(TaskDto::isActive)
                .sorted(this::compare)
                .limit(batchSize)
                .map(TaskDto::getTask)
                .toList()
        );
        Collections.shuffle(nextTasks);
        return Optional.of(nextTasks);
    }

    @Override
    public HtmlElem renderLessParams(boolean historyUpdated) {
        CountAndStreak countAndStreak = calcCountAndStreak(getTaskDtos());
        return frag(
            text(String.format("%s: ", RepeatStrategyType.QUEUE)),
            text(format(
                "Counts: %s | %s/%s",
                countAndStreak.getTotalCount(), countAndStreak.getMinCount(), countAndStreak.getMaxCount()
            )),
            text(format("Streak: %s/%s", countAndStreak.getMinStreak(), countAndStreak.getMaxStreak()))
        );
    }

    @Override
    public HtmlElem renderMoreParams(boolean historyUpdated) {
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
        List<Pair<ArrayList<TaskDto>, ArrayList<TaskDto>>> buckets =
            bucketDelays.stream()
                .map(_ -> Pair.of(new ArrayList<TaskDto>(), new ArrayList<TaskDto>()))
                .toList();
        List<TaskDto> allTasks = getTaskDtos();
        for (TaskDto task : allTasks) {
            Pair<ArrayList<TaskDto>, ArrayList<TaskDto>> bucket = buckets.get(task.getBucketIdx());
            if (task.isActive()) {
                bucket.getRight().add(task);
            } else {
                bucket.getLeft().add(task);
            }
        }
        for (int b = 0; b < buckets.size(); b++) {
            headerRow.add(text(b + 1));
            int bucketDelay = bucketDelays.get(b);
            delayRow.add(text(bucketDelay));
            int activeCnt = buckets.get(b).getRight().size();
            List<TaskDto> waitingTasks = buckets.get(b).getLeft();
            int waitingCnt = waitingTasks.size();
            activeRow.add(text(activeCnt));
            if (activeCnt == 0 && waitingCnt > 0) {
                int minRemainingDelay = waitingTasks.stream()
                    .map(TaskDto::getRemainingDelayExn)
                    .min(Integer::compareTo)
                    .get();
                waitingRow.add(text(format("%s (%s)", waitingCnt, minRemainingDelay)));
            } else {
                waitingRow.add(text(waitingCnt));
            }
            totalRow.add(text(activeCnt + waitingCnt));
        }
        CountAndStreak countAndStreak = calcCountAndStreak(allTasks);
        return frag(
            div(text(format("Repeat strategy: %s", RepeatStrategyType.QUEUE))),
            div(text(format("Directories: %s", getDirectoriesStr()))),
            div(text(format("Task types: %s", getTaskTypesStr()))),
            div(text(format("Number of tasks: %s", allTasks.size()))),
            div(text(format("Batch size: %s", batchSize))),
            div(text(format("Step: %s", step))),
            div(text(format("Step multiplication factor: %s", stepMultFactor))),
            div(text(format(
                "Session counts total|min/max : %s | %s/%s",
                countAndStreak.getTotalCount(), countAndStreak.getMinCount(), countAndStreak.getMaxCount()
            ))),
            div(text(format(
                "Session min/max streak: %s/%s",
                countAndStreak.getMinStreak(), countAndStreak.getMaxStreak()
            ))),
            div(rndTableWithHistLenStat(getHistLengths(allTasks, maxHistLenStat))),
            div(table(rows).attr("class", "table-single-border bucket-params"))
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

    private CountAndStreak calcCountAndStreak(List<TaskDto> allTasks) {
        List<List<HistRec>> sessionHist = allTasks.stream()
            .map(TaskDto::getTask)
            .map(Task::getHist)
            .map(h -> h.getRecords().stream().filter(r -> startTimeForParams.isBefore(r.getTime())).toList())
            .toList();
        Pair<Integer, Integer> count = utils.getMinMax(sessionHist, List::size, Integer::compareTo, Pair.of(0, 0));
        Pair<Integer, Integer> streak = utils.getMinMax(
            sessionHist, utils::getStreak, Integer::compareTo, Pair.of(0, 0)
        );
        return CountAndStreak.builder()
            .totalCount(sessionHist.stream().map(List::size).reduce(0, Integer::sum))
            .minCount(count.getLeft())
            .maxCount(count.getRight())
            .minStreak(streak.getLeft())
            .maxStreak(streak.getRight())
            .build();
    }

    private int compare(TaskDto a, TaskDto b) {
        TaskDto min = strictMin(a, b);
        if (min == a) {
            return -1;
        }
        if (min == b) {
            return 1;
        }
        return 0;
    }

    private TaskDto strictMin(TaskDto a, TaskDto b) {
        // Prefer the task from a bucket with lower index.
        if (a.getBucketIdx() < b.getBucketIdx()) {
            return a;
        }
        if (b.getBucketIdx() < a.getBucketIdx()) {
            return b;
        }

        // If both tasks are not in the last bucket.
        // The last bucket would contain either new tasks or well remembered tasks.
        // Hence, tasks which are not in the last bucket are not new and not remembered well.
        if (a.getBucketIdx() < maxBucketIdx) {
            // Select the task with the longest history to concentrate on one of them first.
            // When it becomes remembered better we can switch to another task.
            if (a.getHistLen() > b.getHistLen()) {
                return a;
            }
            if (b.getHistLen() > a.getHistLen()) {
                return b;
            }
            return null;
        }

        // Among the tasks in the last bucket, select the one with the least remaining delay.
        if (a.getRemainingDelayExn() < b.getRemainingDelayExn()) {
            return a;
        }
        if (b.getRemainingDelayExn() < a.getRemainingDelayExn()) {
            return b;
        }
        return null;
    }

    private List<TaskDto> getTaskDtos() {
        List<TaskDto> allTasks = getAllTasks().stream().map(this::makeTaskDto).toList();
        List<HistRecDto> allHistRev = allTasks.stream()
            .flatMap(task ->
                task.getTask().getHist().getRecords().stream().map(histRec -> makeHistRecDto(task, histRec))
            )
            .sorted(Comparator.comparing(HistRecDto::getTime).reversed())
            .toList();
        int checkedTasksCnt = 0;
        for (int i = 0; i < allHistRev.size() && checkedTasksCnt < allTasks.size(); i++) {
            TaskDto task = allHistRev.get(i).getTask();
            if (task.getRemainingDelay().isPresent()) {
                continue;
            }
            task.setRemainingDelay(Optional.of(task.getBucketDelay() - i));
            checkedTasksCnt++;
        }
        if (checkedTasksCnt < allTasks.size()) {
            // Tasks without remainingDelay don't have history.
            // Setting for them remainingDelay to the minimal value for them to be picked by strictMin().
            Integer remainingDelayForNewTasks = allTasks.stream()
                .map(TaskDto::getRemainingDelay)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .min(Integer::compareTo)
                .filter(minRemainingDelay -> minRemainingDelay <= 0)
                .map(i -> i - 1)
                .orElse(0);
            allTasks.stream()
                .filter(t -> t.getRemainingDelay().isEmpty())
                .forEach(t -> t.setRemainingDelay(Optional.of(remainingDelayForNewTasks)));
        }
        return allTasks;
    }

    private int getBucketIdx(List<HistRec> hist) {
        if (hist.isEmpty() || hist.stream().allMatch(HistRec::isPassed)) {
            return maxBucketIdx;
        }
        return utils.getStreak(hist, maxBucketIdx);
    }

    private TaskDto makeTaskDto(Task task) {
        List<HistRec> hist = task.getHist().getRecords();
        int bucketIdx = getBucketIdx(hist);
        return TaskDto.builder()
            .task(task)
            .histLen(hist.size())
            .bucketIdx(bucketIdx)
            .bucketDelay(bucketDelays.get(bucketIdx))
            .build();
    }

    private static HistRecDto makeHistRecDto(TaskDto task, HistRec histRec) {
        return HistRecDto.builder()
            .histRec(histRec)
            .time(histRec.getTime())
            .task(task)
            .build();
    }

    private List<Integer> getHistLengths(List<TaskDto> allTasks, int maxHistLen) {
        List<Integer> res = new ArrayList<>(maxHistLen + 1);
        for (int i = 0; i <= maxHistLen; i++) {
            res.add(0);
        }
        allTasks.forEach(task -> {
            int len = task.getHistLen();
            int idx = Math.min(len, maxHistLen);
            res.set(idx, res.get(idx) + 1);
        });
        return res;
    }

    private HtmlElem rndTableWithHistLenStat(List<Integer> histLenStat) {
        List<HtmlElem> row1 = new ArrayList<>();
        List<HtmlElem> row2 = new ArrayList<>();
        row1.add(text("History length"));
        row2.add(text("Number of tasks"));
        for (int i = 0; i < histLenStat.size(); i++) {
            if (i < histLenStat.size() - 1) {
                row1.add(text(i));
            } else {
                row1.add(text(">= " + i));
            }
            row2.add(text(histLenStat.get(i)));
        }
        return table(List.of(row1, row2)).attr("class", "table-single-border bucket-params");
    }

    @Getter
    @Builder
    protected static class TaskDto {
        private Task task;
        private int histLen;
        private int bucketIdx;
        private int bucketDelay;
        @Setter
        @Builder.Default
        private Optional<Integer> remainingDelay = Optional.empty();

        public int getRemainingDelayExn() {
            return remainingDelay.orElseThrow(() -> new Exn("The remainingDelay property is not set."));
        }

        public boolean isActive() {
            return getRemainingDelayExn() <= 0;
        }
    }

    @Getter
    @Builder
    private static class HistRecDto {
        private HistRec histRec;
        private Instant time;
        private TaskDto task;
    }

    @Getter
    @Builder
    private static class CountAndStreak {
        private Integer totalCount;
        private Integer minCount;
        private Integer maxCount;
        private Integer minStreak;
        private Integer maxStreak;
    }
}
