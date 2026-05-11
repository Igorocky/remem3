package org.igye.remem3.app.repeatstrategy.impl;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static java.lang.String.format;

public class RepeatStrategyQueue extends HtmlBuilder implements RepeatStrategy {

    public static final int MIN_BATCH_SIZE = 1;
    public static final int MAX_BATCH_SIZE = 10;
    public static final int DEFAULT_BATCH_SIZE = 5;
    public static final int MIN_STEP = 1;
    public static final int MAX_STEP = 20;
    public static final int DEFAULT_STEP = 5;

    private final Utils utils;
    private final int batchSize;
    private final int step;
    private final List<Task> allTasks;
    private final List<Integer> bucketDelays;
    private final int maxBucketNum;

    public RepeatStrategyQueue(Utils utils, int batchSize, int step, List<Task> allTasks) {
        if (CollectionUtils.isEmpty(allTasks)) {
            throw new Exn("There are no tasks.");
        }
        this.utils = utils;
        this.batchSize = utils.getInRange(MIN_BATCH_SIZE, batchSize, MAX_BATCH_SIZE);
        this.step = utils.getInRange(MIN_STEP, step, MAX_STEP);
        this.allTasks = Collections.unmodifiableList(allTasks);
        List<Integer> bucketDelays = new ArrayList<>();
        int maxDelay = utils.getInRange(0, allTasks.size() - batchSize, allTasks.size());
        do {
            if (bucketDelays.isEmpty()) {
                bucketDelays.add(0);
            } else if (bucketDelays.size() == 1) {
                bucketDelays.add(step);
            } else {
                bucketDelays.add(bucketDelays.getLast() * 2);
            }
        } while (bucketDelays.getLast() < maxDelay);
        bucketDelays.set(bucketDelays.size() - 1, maxDelay);
        this.bucketDelays = Collections.unmodifiableList(bucketDelays);
        this.maxBucketNum = bucketDelays.size() - 1;
    }

    @Override
    public Optional<List<Task>> getNextTasks() {
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
            Pair<ArrayList<TaskDto>, ArrayList<TaskDto>> bucket = buckets.get(task.getBucketNum());
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
            div(text(format("Number of tasks: %s", allTasks.size()))),
            div(text(format("Batch size: %s", batchSize))),
            div(text(format("Step: %s", step))),
            div(text(format(
                "Session counts total|min/max : %s | %s/%s",
                countAndStreak.getTotalCount(), countAndStreak.getMinCount(), countAndStreak.getMaxCount()
            ))),
            div(text(format(
                "Session min/max streak: %s/%s",
                countAndStreak.getMinStreak(), countAndStreak.getMaxStreak()
            ))),
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
        TaskDto min = min(a, b);
        if (min == a) {
            return -1;
        }
        if (min == b) {
            return 1;
        }
        return 0;
    }

    private TaskDto min(TaskDto a, TaskDto b) {
        if (a.getBucketNum() < b.getBucketNum()) {
            return a;
        }
        if (b.getBucketNum() < a.getBucketNum()) {
            return b;
        }
        if (a.getBucketNum() < maxBucketNum) {
            if (a.getHistLen() > b.getHistLen()) {
                return a;
            }
            if (b.getHistLen() > a.getHistLen()) {
                return b;
            }
            return null;
        }
        if (a.getRemainingDelayExn() < b.getRemainingDelayExn()) {
            return a;
        }
        if (b.getRemainingDelayExn() < a.getRemainingDelayExn()) {
            return b;
        }
        return null;
    }

    private List<TaskDto> getTaskDtos() {
        List<TaskDto> allTasks = this.allTasks.stream().map(this::makeTaskDto).toList();
        List<HistRecDto> allHistRev = allTasks.stream()
            .flatMap(task -> task.getTask().getHist().stream().map(histRec -> makeHistRecDto(task, histRec)))
            .sorted(Comparator.comparing(HistRecDto::getTime).reversed())
            .toList();
        int checkedTasks = 0;
        for (int i = 0; i < allHistRev.size() && checkedTasks < allTasks.size(); i++) {
            TaskDto task = allHistRev.get(i).getTask();
            if (task.getRemainingDelay().isPresent()) {
                continue;
            }
            task.setRemainingDelay(Optional.of(task.getBucketDelay() - i));
            checkedTasks++;
        }
        if (checkedTasks < allTasks.size()) {
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

    private int getBucketNum(List<HistRec> hist) {
        if (hist.isEmpty() || hist.stream().allMatch(HistRec::isPassed)) {
            return maxBucketNum;
        }
        return utils.getStreak(hist, maxBucketNum);
    }

    private TaskDto makeTaskDto(Task task) {
        List<HistRec> hist = task.getHist();
        int bucketNum = getBucketNum(hist);
        return TaskDto.builder()
            .task(task)
            .histLen(hist.size())
            .bucketNum(bucketNum)
            .bucketDelay(bucketDelays.get(bucketNum))
            .build();
    }

    private static HistRecDto makeHistRecDto(TaskDto task, HistRec histRec) {
        return HistRecDto.builder()
            .histRec(histRec)
            .time(histRec.getTime())
            .task(task)
            .build();
    }

    @Getter
    @Builder
    protected static class TaskDto {
        private Task task;
        private int histLen;
        private int bucketNum;
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
