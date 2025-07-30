package org.igye.remem3.app.impl;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.tuple.Pair;
import org.igye.remem3.app.RepeatStrategy;
import org.igye.remem3.app.dto.HistRec;
import org.igye.remem3.app.dto.Task;
import org.igye.remem3.html.HtmlBuilder;
import org.igye.remem3.html.HtmlElem;
import org.igye.remem3.utils.Exn;
import org.igye.remem3.utils.Utils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class RepeatStrategyQueue extends HtmlBuilder implements RepeatStrategy {

    public static final int MIN_BATCH_SIZE = 1;
    public static final int MAX_BATCH_SIZE = 10;
    public static final int DEFAULT_BATCH_SIZE = 5;
    public static final int MIN_STEP = 1;
    public static final int MAX_STEP = 10;
    public static final int DEFAULT_STEP = 5;
    private final int batchSize;
    private final int step;
    private final List<Task> allTasks;
    private ArrayList<Integer> steps = new ArrayList<>();

    public RepeatStrategyQueue(Utils utils, int batchSize, int step, List<Task> allTasks) {
        this.step = utils.getInRange(MIN_STEP, step, MAX_STEP);
        this.batchSize = utils.getInRange(MIN_BATCH_SIZE, batchSize, MAX_BATCH_SIZE);
        this.allTasks = Collections.unmodifiableList(allTasks);
    }

    @Override
    public Optional<List<Task>> getNextTasks() {
        List<TaskDto> allTasks = getTasksWithStreakAndActivityFlag();
        ArrayList<Task> nextTasks = allTasks.stream()
            .filter(TaskDto::isActiveExn)
            //first repeat all the previously remembered tasks, then proceed to the new ones
            .sorted(Comparator.comparing(TaskDto::getHistLen).reversed().thenComparing(TaskDto::getStreak))
            .limit(batchSize)
            .map(TaskDto::getTask)
            .collect(Collectors.toCollection(ArrayList::new));
        if (nextTasks.isEmpty()) {
            nextTasks = allTasks.stream()
                .sorted(Comparator.comparing(TaskDto::getStreak))
                .limit(batchSize)
                .map(TaskDto::getTask)
                .collect(Collectors.toCollection(ArrayList::new));
        }
        Collections.shuffle(nextTasks);
        return Optional.of(nextTasks);
    }

    @Override
    public HtmlElem renderParams(boolean historyUpdated) {
        List<TaskDto> allTasks = getTasksWithStreakAndActivityFlag();
        String streaks = allTasks.stream()
            .collect(Collectors.toMap(
                TaskDto::getStreak,
                (TaskDto task) -> task.isActiveExn() ? Pair.of(0, 1) : Pair.of(1, 0),
                (cnt1, cnt2) -> Pair.of(cnt1.getLeft() + cnt2.getLeft(), cnt1.getRight() + cnt2.getRight())
            ))
            .entrySet()
            .stream()
            .sorted(Map.Entry.comparingByKey())
            .map(entry -> format("%s(%s/%s)", entry.getKey(), entry.getValue().getLeft(), entry.getValue().getRight()))
            .collect(Collectors.joining(" "));
        return frag(
            div(text(format("Number of tasks: %s", this.allTasks.size()))),
            div(text(format("Batch size: %s", batchSize))),
            div(text(format("Step: %s", step))),
            div(text(format("Streaks: %s", streaks)))
        );
    }

    private List<TaskDto> getTasksWithStreakAndActivityFlag() {
        List<TaskDto> allTasks = this.allTasks.stream().map(this::makeTaskDto).toList();
        List<HistRecDto> allHistRev = allTasks.stream()
            .flatMap(task -> task.getHist().stream().map(histRec -> makeHistRecDto(task, histRec)))
            .sorted(Comparator.comparing(HistRecDto::getTime).reversed())
            .toList();
        int checkedTasks = 0;
        for (int i = 0; i < allHistRev.size() && checkedTasks < allTasks.size(); i++) {
            TaskDto task = allHistRev.get(i).getTask();
            if (task.getIsActive().isPresent()) {
                continue;
            }
            task.setIsActive(Optional.of(task.getStreak() <= i));
            checkedTasks++;
        }
        return allTasks;
    }

    private static HistRecDto makeHistRecDto(TaskDto task, HistRec histRec) {
        return HistRecDto.builder()
            .histRec(histRec)
            .time(histRec.getTime())
            .task(task)
            .build();
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
            .streak(countStreak(hist))
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
        while (steps.size() < res + 1) {
            if (steps.isEmpty()) {
                steps.add(0);
            } else if (steps.size() == 1 || steps.size() == 2) {
                steps.add(step);
            } else {
                steps.add(steps.getLast() * 2);
            }
        }
        return Math.min(steps.get(res), allTasks.size() * step);
    }

    @Getter
    @Builder
    protected static class TaskDto {
        private Task task;
        private String id;
        private List<HistRec> hist;
        private int histLen;
        private int streak;
        @Setter
        @Builder.Default
        private Optional<Boolean> isActive = Optional.empty();

        public boolean isActiveExn() {
            return isActive.orElseThrow(() -> new Exn("The isActive flag is not set."));
        }
    }

    @Getter
    @Builder
    private static class HistRecDto {
        private HistRec histRec;
        private Instant time;
        private TaskDto task;
    }
}
