package org.igye.remem3.app.impl;

import lombok.Builder;
import lombok.Getter;
import org.igye.remem3.app.RepeatStrategy;
import org.igye.remem3.app.dto.HistRec;
import org.igye.remem3.app.dto.Task;
import org.igye.remem3.html.HtmlBuilder;
import org.igye.remem3.html.HtmlElem;
import org.igye.remem3.utils.Utils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class RepeatStrategyQueue extends HtmlBuilder implements RepeatStrategy {

    public static final int MIN_BATCH_SIZE = 1;
    public static final int MAX_BATCH_SIZE = 10;
    public static final int DEFAULT_BATCH_SIZE = 5;
    public static final int MIN_STEP = 1;
    public static final int MAX_STEP = 10;
    private final int batchSize;
    private final int step;
    private final List<Task> allTasks;
    private final int maxStreak;

    public RepeatStrategyQueue(Utils utils, int batchSize, int step, List<Task> allTasks) {
        this.step = utils.getInRange(MIN_STEP, step, MAX_STEP);
        this.batchSize = utils.getInRange(MIN_BATCH_SIZE, batchSize, MAX_BATCH_SIZE);
        this.allTasks = Collections.unmodifiableList(allTasks);
        maxStreak = this.allTasks.size() - this.batchSize;
    }

    @Override
    public Optional<List<Task>> getNextTasks() {
        List<TaskDto> allTasks = this.allTasks.stream().map(this::makeTaskDto).toList();
        List<HistRecDto> allHistRev = allTasks.stream()
            .flatMap(task -> task.getHist().stream().map(histRec -> makeHistRecDto(task, histRec)))
            .sorted(Comparator.comparing(HistRecDto::getTime).reversed())
            .toList();
        Set<String> inactiveTasks = new HashSet<>();
        Set<String> checkedTasks = new HashSet<>();
        for (int i = 0; i < allTasks.size() && i < allHistRev.size(); i++) {
            TaskDto task = allHistRev.get(i).getTask();
            if (checkedTasks.contains(task.getId())) {
                continue;
            }
            if (task.getStreak() > i) {
                inactiveTasks.add(task.getId());
            }
            checkedTasks.add(task.getId());
        }
        ArrayList<Task> nextTasks = allTasks.stream()
            .filter(task -> !inactiveTasks.contains(task.getId()))
            .sorted(Comparator.comparing(TaskDto::getHistLen).reversed())
            .map(TaskDto::getTask)
            .limit(batchSize)
            .collect(Collectors.toCollection(ArrayList::new));
        Collections.shuffle(nextTasks);
        return Optional.of(nextTasks);
    }

    private static HistRecDto makeHistRecDto(TaskDto task, HistRec histRec) {
        return HistRecDto.builder()
            .histRec(histRec)
            .time(histRec.getTime())
            .task(task)
            .build();
    }

    @Override
    public HtmlElem renderParams(boolean historyUpdated) {
        String minStreaks = allTasks.stream()
            .collect(Collectors.toMap(
                task -> countStreak(getHistForTask(task)),
                _ -> 1,
                Integer::sum
            ))
            .entrySet()
            .stream()
            .sorted(Map.Entry.comparingByKey())
//            .limit(10)
            .map(entry -> format("%s(%s)", entry.getKey(), entry.getValue()))
            .collect(Collectors.joining(", "));
        return frag(
            div(text(format("Number of tasks: %s", allTasks.size()))),
            div(text(format("Batch size: %s", batchSize))),
            div(text(format("Step: %s", step))),
            div(text(format("Streaks (streak(tasks)): %s", minStreaks)))
        );
    }

    private List<HistRec> getHistForTask(Task task) {
        String taskTypeCode = task.getTaskType().getCode();
        return task.getCard().getHistory().stream()
            .filter(h -> h.getTaskType().equals(taskTypeCode))
            .toList();
    }

    private TaskDto makeTaskDto(Task task) {
        List<HistRec> hist = getHistForTask(task);
        return TaskDto.builder()
            .task(task)
            .id(task.getId())
            .hist(hist)
            .histLen(hist.size())
            .streak(Math.min(countStreak(hist) * step, maxStreak))
            .build();
    }


    protected int countStreak(List<HistRec> hist) {
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
    protected static class TaskDto {
        private Task task;
        private String id;
        private List<HistRec> hist;
        private int histLen;
        private int streak;
    }

    @Getter
    @Builder
    private static class HistRecDto {
        private HistRec histRec;
        private Instant time;
        private TaskDto task;
    }
}
