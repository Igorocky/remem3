package org.igye.remem3.app.controllers2.beans;

import org.igye.remem3.app.TaskTypeMatcher;
import org.igye.remem3.app.controllers2.beans.dto.TaskFilter;
import org.igye.remem3.app.impl.TaskTypeMatcherImpl;

import java.util.Arrays;

public class Functions {

    public static TaskFilter taskType(String... types) {
        TaskTypeMatcher taskTypeMatcher = Arrays.stream(types)
            .map(TaskTypeMatcherImpl::new)
            .map(TaskTypeMatcher.class::cast)
            .reduce(TaskTypeMatcher::or)
            .get();
        return task -> taskTypeMatcher.matches(task.getTaskType());
    }
}
