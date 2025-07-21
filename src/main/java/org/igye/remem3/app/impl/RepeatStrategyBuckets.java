package org.igye.remem3.app.impl;

import lombok.Builder;
import lombok.Getter;
import org.apache.commons.lang3.tuple.Pair;
import org.igye.remem3.app.RepeatStrategy;
import org.igye.remem3.app.dto.HistRec;
import org.igye.remem3.app.dto.Task;
import org.igye.remem3.html.HtmlBuilder;
import org.igye.remem3.html.HtmlElem;
import org.igye.remem3.utils.Exn;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

public class RepeatStrategyBuckets extends HtmlBuilder implements RepeatStrategy {

    private static final int NUM_OF_TASKS_TO_SELECT = 5;
    private final Clock clock;
    private final List<Task> allTasks;
    private final List<Duration> bucketDelays;
    private final boolean useBucketForNewTasks;
    private final Random rnd;

    public RepeatStrategyBuckets(
        Clock clock,
        List<Task> allTasks,
        List<Duration> bucketDelays,
        boolean useBucketForNewTasks
    ) {
        this.clock = clock;
        this.allTasks = Collections.unmodifiableList(allTasks);
        this.bucketDelays = Collections.unmodifiableList(bucketDelays);
        this.useBucketForNewTasks = useBucketForNewTasks;
        rnd = new Random();
    }

    @Override
    public Optional<List<Task>> getNextTasks() {
        Stats stats = getStats();
        Map<String, List<HistRec>> taskToHist = stats.getTaskToHist();
        List<Task> activeTasks = stats.getBuckets().stream()
            .map(Pair::getRight)
            .flatMap(Collection::stream)
            .sorted(Comparator.comparing(task -> {
                List<HistRec> hist = taskToHist.get(task.getId());
                return hist.isEmpty() ? Instant.MIN : hist.getLast().getTime();
            }))
            .collect(Collectors.toCollection(ArrayList::new));
        Map<String, Instant> dirToLastTime = activeTasks.stream()
            .collect(Collectors.toMap(
                Task::getDir,
                task -> {
                    List<HistRec> hist = taskToHist.get(task.getId());
                    return hist.isEmpty() ? Instant.MIN : hist.getLast().getTime();
                },
                (t1, t2) -> t1.compareTo(t2) < 0 ? t2 : t1
            ));
        ArrayList<String> preferredDirs = dirToLastTime.entrySet().stream()
            .sorted(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .collect(Collectors.toCollection(ArrayList::new));
        List<Task> selectedTasks = new ArrayList<>();
        mainLoop:
        while (selectedTasks.size() < NUM_OF_TASKS_TO_SELECT && !activeTasks.isEmpty()) {
            String curDir = preferredDirs.getFirst();
            for (int i = 0; i < activeTasks.size(); i++) {
                Task task = activeTasks.get(i);
                if (curDir.equals(task.getDir())) {
                    selectedTasks.add(task);
                    activeTasks.remove(i);
                    preferredDirs.removeFirst();
                    preferredDirs.add(curDir);
                    continue mainLoop;
                }
            }
            throw new Exn(String.format("Cannot find an active task in the directory %s", curDir));
        }
        List<Task> newTasks = stats.getNewTasks();
        if (selectedTasks.size() < NUM_OF_TASKS_TO_SELECT && !newTasks.isEmpty()) {
            Collections.shuffle(newTasks);
            Set<String> knownDirs = new HashSet<>(preferredDirs);
            boolean newTaskFound = false;
            for (Task task : newTasks) {
                if (!knownDirs.contains(task.getDir())) {
                    selectedTasks.add(task);
                    newTaskFound = true;
                    break;
                }
            }
            if (!newTaskFound) {
                dirLoop:
                for (int d = 0; d < preferredDirs.size(); d++) {
                    String dir = preferredDirs.get(d);
                    for (int t = 0; t < newTasks.size(); t++) {
                        Task task = newTasks.get(t);
                        if (dir.equals(task.getDir())) {
                            selectedTasks.add(task);
                            newTaskFound = true;
                            break dirLoop;
                        }
                    }
                }
                if (!newTaskFound) {
                    throw new Exn("Cannot find a new task.");
                }
            }
        }
        return Optional.of(shuffleTasksWithinDirs(selectedTasks));
    }

    private List<Task> shuffleTasksWithinDirs(List<Task> tasks) {
        Map<String, List<Task>> dirToTasks = tasks.stream()
            .collect(Collectors.groupingBy(Task::getDir, Collectors.toCollection(ArrayList::new)));
        dirToTasks.values().forEach(Collections::shuffle);
        return tasks.stream()
            .map(t -> dirToTasks.get(t.getDir()).removeFirst())
            .toList();
    }

    @Override
    public HtmlElem renderParams(boolean historyUpdated) {
        Stats stats = getStats();
        throw new Exn("not implemented");
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
        List<Instant> bucketActivationTimes = bucketDelays.stream()
            .map(curTime::plus)
            .toList();
        for (Task task : allTasks) {
            List<HistRec> taskHist = taskToHist.get(task.getId());
            if (taskHist.isEmpty() && useBucketForNewTasks) {
                newTasks.add(task);
            } else {
                int bucketNum = getBucketNum(taskHist);
                Pair<List<Task>, List<Task>> bucket = buckets.get(bucketNum);
                if (!taskHist.isEmpty()
                    && taskHist.getLast().getTime().isBefore(bucketActivationTimes.get(bucketNum))
                ) {
                    bucket.getLeft().add(task);
                } else {
                    bucket.getRight().add(task);
                }
            }
        }
        return Stats.builder()
            .taskToHist(taskToHist)
            .newTasks(newTasks)
            .buckets(buckets)
            .build();
    }

    private int getBucketNum(List<HistRec> hist) {
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
