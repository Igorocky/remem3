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
    private final List<Task> allTasks;
    private final int maxStreak;

    public RepeatStrategyQueue(Utils utils, int batchSize, List<Task> allTasks) {
        this.batchSize = utils.getInRange(MIN_BATCH_SIZE, batchSize, MAX_BATCH_SIZE);
        this.allTasks = Collections.unmodifiableList(allTasks);
        maxStreak = this.allTasks.size() - this.batchSize;
    }

    @Override
    public Optional<List<Task>> getNextTasks() {
        List<TaskDto> allTasks = this.allTasks.stream()
            .map(t -> {
                String taskTypeCode = t.getTaskType().getCode();
                List<HistRec> hist =
                    t.getCard().getHistory().stream().filter(h -> h.getTaskType().equals(taskTypeCode)).toList();
                return TaskDto.builder()
                    .task(t)
                    .id(t.getId())
                    .hist(hist)
                    .histLen(hist.size())
                    .streak(Math.min(countStreak(hist), maxStreak))
                    .build();
            })
            .sorted(Comparator.comparing(TaskDto::getHistLen).reversed())//move new tasks to the end
            .toList();
        List<HistRecDto> allHistRev = allTasks.stream()
            .flatMap(task ->
                task.getHist().stream()
                    .map(histRec -> HistRecDto.builder().histRec(histRec).task(task).build())
            )
            .sorted(Comparator.comparing((HistRecDto histRecDto) -> histRecDto.getHistRec().getTime()).reversed())
            .toList();
        Set<String> inactiveTasks = new HashSet<>();
        Set<String> activeTasks = new HashSet<>();
        int histIdx = -1;
        for (HistRecDto histRec : allHistRev) {
            histIdx++;
            if (histIdx >= allTasks.size()) {
                break;
            }
            TaskDto task = histRec.getTask();
            if (inactiveTasks.contains(task.getId()) || activeTasks.contains(task.getId())) {
                continue;
            }
            boolean streakConditionMet = task.getStreak() <= histIdx;
            if (streakConditionMet) {
                activeTasks.add(task.getId());
            } else {
                inactiveTasks.add(task.getId());
            }
        }
        ArrayList<Task> nextTasks = allTasks.stream()
            .filter(task -> !inactiveTasks.contains(task.getId()))
            .sorted(Comparator.comparing(TaskDto::getStreak))//first repeat hard to memorize tasks
            .map(TaskDto::getTask)
            .limit(batchSize)
            .collect(Collectors.toCollection(ArrayList::new));
        Collections.shuffle(nextTasks);
        return Optional.of(nextTasks);
    }

    @Override
    public HtmlElem renderParams(boolean historyUpdated) {
        return frag(
            div(text(format("Number of tasks: %s", allTasks.size()))),
            div(text(format("Batch size: %s", batchSize)))
        );
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
    private static class TaskDto {
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
        private TaskDto task;
    }
}
