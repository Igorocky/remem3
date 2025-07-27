package org.igye.remem3.app.impl;

import lombok.Builder;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.igye.remem3.app.RepeatStrategy;
import org.igye.remem3.app.dto.HistRec;
import org.igye.remem3.app.dto.Task;
import org.igye.remem3.html.HtmlBuilder;
import org.igye.remem3.html.HtmlElem;
import org.igye.remem3.html.HtmlText;
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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;

public class RepeatStrategyBuckets extends HtmlBuilder implements RepeatStrategy {

    public static final int MIN_BATCH_SIZE = 1;
    public static final int MAX_BATCH_SIZE = 10;
    public static final int DEFAULT_BATCH_SIZE = 5;
    private final Utils utils;
    private final Clock clock;
    private final int batchSize;
    private final boolean preferTasksWithLongerHistory;
    private final List<Task> allTasks;
    private final List<Duration> bucketDelays;
    private final boolean useBucketForNewTasks;

    public RepeatStrategyBuckets(
        Utils utils,
        Clock clock,
        int batchSize,
        boolean preferTasksWithLongerHistory,
        List<Task> allTasks,
        List<Duration> bucketDelays,
        boolean useBucketForNewTasks
    ) {
        this.utils = utils;
        this.clock = clock;
        this.batchSize = Math.max(MIN_BATCH_SIZE, Math.min(batchSize, MAX_BATCH_SIZE));
        this.preferTasksWithLongerHistory = preferTasksWithLongerHistory;
        this.allTasks = Collections.unmodifiableList(allTasks);
        this.bucketDelays = Collections.unmodifiableList(bucketDelays);
        this.useBucketForNewTasks = useBucketForNewTasks;
    }

    @Override
    public Optional<List<Task>> getNextTasks() {
        Stats stats = getStats();
        Map<String, List<HistRec>> taskToHist = stats.getTaskToHist();
        ArrayList<Task> activeTasks = getPreferredActiveTasks(clock.instant(), stats.getBuckets(), taskToHist);
        ArrayList<String> preferredDirs = getPreferredDirs(activeTasks, taskToHist);
        List<Task> selectedTasks = selectActiveTasksToRepeat(activeTasks, preferredDirs, batchSize);
        List<Task> newTasks = stats.getNewTasks();
        if (selectedTasks.size() < batchSize && !newTasks.isEmpty()) {
            Collections.shuffle(newTasks);
            selectedTasks.add(selectNewTask(activeTasks, preferredDirs, selectedTasks, newTasks));
        }
        Collections.shuffle(selectedTasks);
        return Optional.of(selectedTasks);
    }

    @Override
    public HtmlElem renderParams(boolean historyUpdated) {
        Stats stats = getStats();
        ArrayList<List<? extends HtmlElem>> rows = new ArrayList<>();
        List<HtmlElem> header = new ArrayList<>();
        List<HtmlElem> delay = new ArrayList<>();
        List<HtmlElem> active = new ArrayList<>();
        List<HtmlElem> waiting = new ArrayList<>();
        List<HtmlElem> total = new ArrayList<>();
        rows.add(header);
        rows.add(delay);
        rows.add(active);
        rows.add(waiting);
        rows.add(total);
        header.add(text("Bucket number"));
        delay.add(text("Bucket delay"));
        active.add(text("Active"));
        waiting.add(text("Waiting"));
        total.add(text("Total"));
        List<Pair<List<Task>, List<Task>>> buckets = stats.getBuckets();
        List<Task> newTasks = stats.getNewTasks();
        Instant curTime = clock.instant();
        Map<String, List<HistRec>> taskToHist = stats.getTaskToHist();
        for (int b = 0; b < buckets.size(); b++) {
            if (b == 0 && !newTasks.isEmpty()) {
                header.add(text("0"));
                delay.add(null);
                HtmlText numOfNewTasksElem = text(String.valueOf(newTasks.size()));
                active.add(numOfNewTasksElem);
                waiting.add(null);
                total.add(numOfNewTasksElem);
            }
            header.add(text(String.valueOf(b + 1)));
            Duration bucketDelay = bucketDelays.get(b);
            delay.add(text(utils.durationToStr(bucketDelay)));
            int activeCnt = buckets.get(b).getRight().size();
            List<Task> waitingTasks = buckets.get(b).getLeft();
            int waitingCnt = waitingTasks.size();
            active.add(text(String.valueOf(activeCnt)));
            if (activeCnt == 0 && waitingCnt > 0) {
                Duration maxTaskDelay = waitingTasks.stream()
                    .map(t -> taskToHist.get(t.getId()))
                    .map(List::getLast)
                    .map(HistRec::getTime)
                    .map(lastTime ->
                        lastTime.isBefore(curTime)
                            ? Duration.between(lastTime, curTime)
                            : Duration.ZERO
                    )
                    .max(Duration::compareTo)
                    .orElseThrow(() -> new Exn("Cannot determine maxTaskDelay."));
                Duration timeToWait = bucketDelay.compareTo(maxTaskDelay) <= 0
                    ? Duration.ZERO
                    : bucketDelay.minus(maxTaskDelay);
                waiting.add(text(format("%s (%s)", waitingCnt, getApproxDurationStr(timeToWait))));
            } else {
                waiting.add(text(String.valueOf(waitingCnt)));
            }
            total.add(text(String.valueOf(activeCnt + waitingCnt)));
        }
        return frag(
            div(text(format("Number of tasks: %s", allTasks.size()))),
            div(text(format("Use a separate bucket for new tasks: %s", useBucketForNewTasks ? "Yes" : "No"))),
            div(text(format("Prefer tasks with longer history: %s", preferTasksWithLongerHistory ? "Yes" : "No"))),
            div(text(format("Batch size: %s", batchSize))),
            div(table(rows).attr("class", "table-single-border bucket-params"))
        );
    }

    private Task selectNewTask(
        List<Task> activeTasks,
        List<String> preferredDirs,
        List<Task> selectedTasks,
        List<Task> newTasks
    ) {
        if (preferTasksWithLongerHistory) {
            return newTasks.getFirst();
        } else {
            Set<String> knownDirs = Stream.concat(activeTasks.stream(), selectedTasks.stream())
                .map(Task::getDir)
                .collect(Collectors.toSet());
            for (Task task : newTasks) {
                if (!knownDirs.contains(task.getDir())) {
                    return task;
                }
            }
            if (preferredDirs.isEmpty()) {
                return newTasks.getFirst();
            }
            for (int d = 0; d < preferredDirs.size(); d++) {
                String dir = preferredDirs.get(d);
                for (int t = 0; t < newTasks.size(); t++) {
                    Task task = newTasks.get(t);
                    if (dir.equals(task.getDir())) {
                        return task;
                    }
                }
            }
            return newTasks.getFirst();
        }
    }

    private ArrayList<Task> selectActiveTasksToRepeat(
        ArrayList<Task> activeTasks,
        ArrayList<String> preferredDirs,
        int maxNumOfTasksToSelect
    ) {
        ArrayList<Task> selectedTasks = new ArrayList<>();
        if (preferTasksWithLongerHistory) {
            selectedTasks.addAll(
                activeTasks.stream().limit(maxNumOfTasksToSelect).toList()
            );
        } else {
            mainLoop:
            while (selectedTasks.size() < maxNumOfTasksToSelect && !activeTasks.isEmpty()) {
                String curDir = preferredDirs.getFirst();
                for (int i = 0; i < activeTasks.size(); i++) {
                    Task task = activeTasks.get(i);
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
        }
        return selectedTasks;
    }

    private ArrayList<Task> getPreferredActiveTasks(
        Instant curTime,
        List<Pair<List<Task>, List<Task>>> buckets,
        Map<String, List<HistRec>> taskToHist
    ) {
        List<Pair<Task, BigDecimal>> overdues = calcOverdues(curTime, buckets, taskToHist);
        Comparator<Pair<Task, BigDecimal>> overdueCmp = Comparator.<Pair<Task, BigDecimal>, BigDecimal>comparing(
            Pair::getRight
        ).reversed();
        Comparator<Pair<Task, BigDecimal>> taskCmp;
        if (preferTasksWithLongerHistory) {
            taskCmp = Comparator.<Pair<Task, BigDecimal>, Integer>comparing(
                    p -> taskToHist.get(p.getLeft().getId()).size()
                )
                .reversed().thenComparing(overdueCmp);
        } else {
            taskCmp = overdueCmp;
        }
        return overdues.stream()
            .sorted(taskCmp)
            .map(Pair::getLeft)
            .collect(Collectors.toCollection(ArrayList::new));
    }

    private ArrayList<String> getPreferredDirs(
        List<Task> activeTasks, Map<String, List<HistRec>> taskToHist
    ) {
        Map<String, Instant> dirToLastTime = activeTasks.stream()
            .collect(Collectors.toMap(
                Task::getDir,
                task -> {
                    List<HistRec> hist = taskToHist.get(task.getId());
                    return hist.isEmpty() ? Instant.MIN : hist.getLast().getTime();
                },
                (t1, t2) -> t1.compareTo(t2) < 0 ? t2 : t1
            ));
        return dirToLastTime.entrySet().stream()
            .sorted(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .collect(Collectors.toCollection(ArrayList::new));
    }

    private List<Pair<Task, BigDecimal>> calcOverdues(
        Instant curTime,
        List<Pair<List<Task>, List<Task>>> buckets,
        Map<String, List<HistRec>> taskToHist
    ) {
        List<Pair<Task, BigDecimal>> overdues = new ArrayList<>();
        for (int b = 0; b < buckets.size(); b++) {
            List<Task> activeTasks = buckets.get(b).getRight();
            for (int t = 0; t < activeTasks.size(); t++) {
                Task task = activeTasks.get(t);
                overdues.add(Pair.of(
                    task,
                    getOverdue(curTime, bucketDelays.get(b), taskToHist.get(task.getId()))
                ));
            }
        }
        return overdues;
    }

    private String getApproxDurationStr(Duration dur) {
        return Arrays.stream(utils.durationToStr(dur).split("\\s+"))
            .filter(StringUtils::isNotBlank)
            .findFirst()
            .get();
    }

    protected BigDecimal getOverdue(Instant curTime, Duration bucketDelay, List<HistRec> hist) {
        if (hist.isEmpty()) {
            return new BigDecimal("0.15");
        }
        Instant lastTime = hist.getLast().getTime();
        Duration taskDuration = Duration.between(lastTime, curTime);
        if (taskDuration.compareTo(bucketDelay) <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(taskDuration.minus(bucketDelay).getSeconds())
            .setScale(10, RoundingMode.HALF_UP)
            .divide(
                BigDecimal.valueOf(bucketDelay.getSeconds()).setScale(10, RoundingMode.HALF_UP), RoundingMode.HALF_UP
            );
    }

    private Stats getStats() {
        Map<String, List<HistRec>> taskToHist = allTasks.stream()
            .collect(Collectors.toMap(
                Task::getId,
                task -> task.getCard().getHistory().stream()
                    .filter(histRec -> Objects.equals(histRec.getTaskType(), task.getTaskType().getCode()))
                    .toList()
            ));
        List<Task> newTasks = new ArrayList<>();
        List<Pair<List<Task>, List<Task>>> buckets = new ArrayList<>(bucketDelays.size());
        for (int i = 0; i < bucketDelays.size(); i++) {
            buckets.add(Pair.of(new ArrayList<>(), new ArrayList<>()));
        }
        Instant curTime = clock.instant();
        int maxBucketNum = buckets.size() - 1;
        for (Task task : allTasks) {
            List<HistRec> taskHist = taskToHist.get(task.getId());
            if (taskHist.isEmpty() && useBucketForNewTasks) {
                newTasks.add(task);
            } else {
                int bucketNum = Math.min(getBucketNum(taskHist), maxBucketNum);
                Pair<List<Task>, List<Task>> bucket = buckets.get(bucketNum);
                if (taskHist.isEmpty()) {
                    bucket.getRight().add(task);
                } else {
                    Instant taskLastTime = taskHist.getLast().getTime();
                    Duration taskDur = Duration.between(taskLastTime, curTime);
                    if (taskDur.compareTo(bucketDelays.get(bucketNum)) > 0) {
                        bucket.getRight().add(task);
                    } else {
                        bucket.getLeft().add(task);
                    }
                }
            }
        }
        return Stats.builder()
            .taskToHist(taskToHist)
            .newTasks(newTasks)
            .buckets(buckets)
            .build();
    }

    protected int getBucketNum(List<HistRec> hist) {
        if (hist.isEmpty() && useBucketForNewTasks) {
            throw new Exn("hist.isEmpty()");
        }
        int res = 0;
        for (int i = hist.size() - 1; i >= 0; i--) {
            if (hist.get(i).getMark().compareTo(BigDecimal.ONE) < 0) {
                break;
            }
            res++;
        }
        return res;
    }

    @Getter
    @Builder
    private static class Stats {
        private Map<String, List<HistRec>> taskToHist;
        private List<Task> newTasks;
        private List<Pair<List<Task>, List<Task>>> buckets;
    }
}
