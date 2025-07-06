package org.igye.remem3.app.impl;

import org.apache.commons.collections4.CollectionUtils;
import org.igye.remem3.app.TaskRepeatStrategy;
import org.igye.remem3.app.dto.HistRec;
import org.igye.remem3.app.dto.Task;
import org.igye.remem3.utils.Exn;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;

public class TaskRepeatStrategyCircle implements TaskRepeatStrategy {

    private final List<Task> allTasks;
    private final double randomnessFactor;
    private final Random rnd;

    public TaskRepeatStrategyCircle(List<Task> allTasks, double randomnessFactor) {
        if (CollectionUtils.isEmpty(allTasks)) {
            throw new Exn("allTasks cannot be empty.");
        }
        this.allTasks = Collections.unmodifiableList(allTasks);
        this.randomnessFactor = randomnessFactor;
        rnd = new Random();
    }

    @Override
    public Task getNextTask() {
        List<Task> tasksToSelectFrom = allTasks.stream()
            .sorted((t1, t2) -> {
                Optional<HistRec> h1Opt = t1.getLastHistRec();
                Optional<HistRec> h2Opt = t2.getLastHistRec();
                if (h1Opt.isEmpty()) {
                    if (h2Opt.isEmpty()) {
                        return 0;
                    } else {
                        return -1;
                    }
                } else {
                    if (h2Opt.isEmpty()) {
                        return 1;
                    } else {
                        return h1Opt.get().getTime().compareTo(h2Opt.get().getTime());
                    }
                }
            })
            .limit(Math.max(1L, Math.round(allTasks.size() * randomnessFactor)))
            .toList();
        return tasksToSelectFrom.get(rnd.nextInt(tasksToSelectFrom.size()));
    }
}
