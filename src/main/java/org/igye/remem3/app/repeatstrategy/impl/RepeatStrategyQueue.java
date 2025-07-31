package org.igye.remem3.app.repeatstrategy.impl;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.tuple.Pair;
import org.igye.remem3.app.repeatstrategy.HistRec;
import org.igye.remem3.app.repeatstrategy.RepeatStrategy;
import org.igye.remem3.app.repeatstrategy.Task;
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
    public static final int MAX_STEP = 20;
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
        Map<Integer, Pair<Pair<Integer, Integer>, Integer>> streaks =
            allTasks.stream()
                .collect(Collectors.toMap(
                    TaskDto::getStreak,
                    (TaskDto task) -> Pair.of(
                        task.isActiveExn() ? Pair.of(0, 1) : Pair.of(1, 0),
                        task.getWaitTillActivationExn()
                    ),
                    (cnt1, cnt2) -> Pair.of(
                        Pair.of(
                            cnt1.getLeft().getLeft() + cnt2.getLeft().getLeft(),
                            cnt1.getLeft().getRight() + cnt2.getLeft().getRight()
                        ),
                        Math.min(cnt1.getRight(), cnt1.getRight())
                    )
                ));

        ArrayList<List<? extends HtmlElem>> rows = new ArrayList<>();
        List<HtmlElem> streakRow = new ArrayList<>();
        List<HtmlElem> activeRow = new ArrayList<>();
        List<HtmlElem> waitRow = new ArrayList<>();
        List<HtmlElem> inactiveRow = new ArrayList<>();
        List<HtmlElem> totalRow = new ArrayList<>();
        rows.add(streakRow);
        rows.add(activeRow);
        rows.add(waitRow);
        rows.add(inactiveRow);
        rows.add(totalRow);
        streakRow.add(text("Streak"));
        activeRow.add(text("Active"));
        waitRow.add(text("Wait"));
        inactiveRow.add(text("Inactive"));
        totalRow.add(text("Total"));

        streaks.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> {
                streakRow.add(text(entry.getKey() + ""));
                Pair<Integer, Integer> counts = entry.getValue().getLeft();
                int inactive = counts.getLeft();
                int active = counts.getRight();
                activeRow.add(text(active + ""));
                waitRow.add(text(entry.getValue().getRight() + ""));
                inactiveRow.add(text(inactive + ""));
                totalRow.add(text((active + inactive) + ""));
            });

        return frag(
            div(text(format("Number of tasks: %s", this.allTasks.size()))),
            div(text(format("Batch size: %s", batchSize))),
            div(text(format("Step: %s", step))),
            div(table(rows).attr("class", "table-single-border bucket-params"))
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
            if (task.getWaitTillActivation().isPresent()) {
                continue;
            }
            task.setWaitTillActivation(Optional.of(task.getStreak() - i));
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

    private TaskDto makeTaskDto(Task task) {
        List<HistRec> hist = task.loadHistory();
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
        private Optional<Integer> waitTillActivation = Optional.empty();

        public int getWaitTillActivationExn() {
            return waitTillActivation.orElseThrow(() -> new Exn("The waitTillActivation flag is not set."));
        }

        public boolean isActiveExn() {
            return getWaitTillActivationExn() <= 0;
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
