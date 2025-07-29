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
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class RepeatStrategyQueue extends HtmlBuilder implements RepeatStrategy {

    public static final int MIN_BATCH_SIZE = 1;
    public static final int MAX_BATCH_SIZE = 10;
    public static final int DEFAULT_BATCH_SIZE = 5;
    private final int batchSize;
    private final int streakThreshold;
    private final List<Task> allTasks;
    private final int maxStreak;

    public RepeatStrategyQueue(Utils utils, int batchSize, int streakThreshold, List<Task> allTasks) {
        this.streakThreshold = streakThreshold;
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

    protected int compare(TaskDto a, TaskDto b) {
        if (a.getHistLen() == 0) {
            //A is a new task
            if (b.getHistLen() == 0) {
                //B is a new task
                return 0;
            } else {
                //B is an old task
                if (b.getStreak() >= streakThreshold) {
                    //B is probably good memorized
                    return -1;
                } else {
                    //B may be not memorized good
                    return 1;
                }
            }
        } else {
            //A is an old task
            if (b.getHistLen() == 0) {
                //B is a new task
                if (a.getStreak() >= streakThreshold) {
                    //A is probably good memorized
                    return 1;
                } else {
                    //A may be not memorized good
                    return -1;
                }
            } else {
                //B is an old task
                if (a.getStreak() < b.getStreak()) {
                    return -1;
                } else if (a.getStreak() == b.getStreak()) {
                    return 0;
                } else {
                    return 1;
                }
            }
        }
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
        return frag(
            div(text(format("Number of tasks: %s", allTasks.size()))),
            div(text(format("Batch size: %s", batchSize)))
        );
    }

    private TaskDto makeTaskDto(Task t) {
        String taskTypeCode = t.getTaskType().getCode();
        List<HistRec> hist = t.getCard().getHistory().stream()
            .filter(h -> h.getTaskType().equals(taskTypeCode))
            .toList();
        return TaskDto.builder()
            .task(t)
            .id(t.getId())
            .hist(hist)
            .histLen(hist.size())
            .streak(Math.min(countStreak(hist), maxStreak))
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
