package org.igye.remem3.app.impl;

import org.apache.commons.collections4.CollectionUtils;
import org.igye.remem3.app.RepeatStrategy;
import org.igye.remem3.app.dto.HistRec;
import org.igye.remem3.app.dto.Task;
import org.igye.remem3.utils.Exn;

import java.time.Instant;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

public class RepeatStrategyCircle implements RepeatStrategy {

    public static final int DEFAULT_MAX_NUM_OF_CIRCLES = 1000_000;
    
    private final List<Task> allTasks;
    private final Instant startTime;
    private final double randomnessFactor;
    private final int maxNumOfCircles;
    private final Random rnd;

    public RepeatStrategyCircle(
        List<Task> allTasks,
        Instant startTime,
        double randomnessFactor,
        int maxNumOfCircles
    ) {
        if (CollectionUtils.isEmpty(allTasks)) {
            throw new Exn("allTasks cannot be empty.");
        }
        this.allTasks = Collections.unmodifiableList(allTasks);
        this.startTime = startTime;
        this.randomnessFactor = Math.max(0, Math.min(randomnessFactor, 1));
        this.maxNumOfCircles = Math.max(1, Math.min(maxNumOfCircles, DEFAULT_MAX_NUM_OF_CIRCLES));
        rnd = new Random();
    }

    @Override
    public Optional<Task> getNextTask() {
        Map<String, List<HistRec>> hist = allTasks.stream()
            .collect(Collectors.toMap(
                Task::getId,
                task ->
                    task.getCard().getHistory().stream()
                        .filter(histRec -> Objects.equals(histRec.getTaskType(), task.getTaskType().getCode()))
                        .filter(histRec -> startTime.isBefore(histRec.getTime()))
                        .sorted(Comparator.comparing(HistRec::getTime))
                        .toList()
            ));
        Map<String, Integer> counts = hist.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().size()));
        int minCnt = counts.values().stream().min(Integer::compareTo).orElseThrow(() ->
            new Exn("allTasks must not be empty.")
        );
        Set<String> taskIdsWithMinCnt = counts.entrySet().stream()
            .filter(e -> {
                int cnt = counts.get(e.getKey());
                return cnt < maxNumOfCircles && cnt == minCnt;
            })
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());
        Map<String, Instant> taskLastTime = hist.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                ent -> ent.getValue().isEmpty() ? startTime : ent.getValue().getLast().getTime()
            ));
        List<Task> tasksToSelectFrom = allTasks.stream()
            .filter(task -> taskIdsWithMinCnt.contains(task.getId()))
            .sorted(Comparator.comparing(task -> taskLastTime.get(task.getId())))
            .limit(Math.max(1L, Math.round(allTasks.size() * randomnessFactor)))
            .toList();
        if (tasksToSelectFrom.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(tasksToSelectFrom.get(rnd.nextInt(tasksToSelectFrom.size())));
        }
    }
}
