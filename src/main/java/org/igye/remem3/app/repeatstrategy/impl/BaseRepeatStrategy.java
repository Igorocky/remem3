package org.igye.remem3.app.repeatstrategy.impl;

import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.igye.remem3.app.controllers.exercise.HasBaseTask;
import org.igye.remem3.app.dto.Card;
import org.igye.remem3.app.dto.TaskType;
import org.igye.remem3.app.repeatstrategy.Hist;
import org.igye.remem3.app.repeatstrategy.HistRec;
import org.igye.remem3.app.repeatstrategy.RepeatStrategy;
import org.igye.remem3.app.repeatstrategy.Task;
import org.igye.remem3.html.HtmlBuilder;
import org.igye.remem3.utils.Exn;

import java.io.File;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public abstract class BaseRepeatStrategy extends HtmlBuilder implements RepeatStrategy {
    private final List<Task> allTasks;
    protected final Instant startTime;
    protected final Optional<Instant> minHistTime;
    private final String directoriesStr;
    private final String taskTypesStr;

    public BaseRepeatStrategy(List<Task> allTasks, Instant startTime) {
        this.allTasks = Collections.unmodifiableList(allTasks);
        this.startTime = startTime;
        this.minHistTime = allTasks.stream()
            .map(Task::getHist)
            .map(Hist::getRecords)
            .flatMap(Collection::stream)
            .map(HistRec::getTime)
            .min(Instant::compareTo);
        if (minHistTime.map(mht -> mht.isBefore(startTime)).orElse(false)) {
            throw new Exn("minHistTime < startTime");
        }
        List<org.igye.remem3.app.dto.Task> baseTasks = allTasks.stream()
            .map(HasBaseTask.class::cast)
            .map(HasBaseTask::getBaseTask)
            .toList();
        List<String> directories = baseTasks.stream()
            .map(org.igye.remem3.app.dto.Task::getCard)
            .map(Card::getFile)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(File::getParentFile)
            .map(this::getCanonicalPath)
            .distinct()
            .sorted()
            .toList();
        List<String> taskTypes = baseTasks.stream()
            .map(org.igye.remem3.app.dto.Task::getTaskType)
            .map(TaskType::getCode)
            .distinct()
            .sorted()
            .toList();
        directoriesStr = StringUtils.join(directories, ", ");
        taskTypesStr = StringUtils.join(taskTypes, ", ");
    }

    protected List<Task> getAllTasks() {
        return allTasks;
    }

    protected String getDirectoriesStr() {
        return directoriesStr;
    }

    protected String getTaskTypesStr() {
        return taskTypesStr;
    }

    @SneakyThrows
    private String getCanonicalPath(File file) {
        return file.getCanonicalPath();
    }
}
