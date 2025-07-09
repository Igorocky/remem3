package org.igye.remem3.app;

import org.apache.commons.collections4.CollectionUtils;
import org.igye.remem3.app.dto.TaskType;
import org.igye.remem3.app.impl.TaskTypeMatcherImpl;

import java.util.Set;

public interface TaskTypeMatcher {
    boolean matches(TaskType taskType);

    default TaskTypeMatcher or(TaskTypeMatcher other) {
        return typ -> this.matches(typ) || other.matches(typ);
    }

    static TaskTypeMatcher fromList(Set<String> typesStr) {
        if (CollectionUtils.isEmpty(typesStr)) {
            return _ -> true;
        }
        return typesStr.stream()
            .map(TaskTypeMatcherImpl::new)
            .map(m -> (TaskTypeMatcher) m)
            .reduce(TaskTypeMatcher::or)
            .get();
    }
}
