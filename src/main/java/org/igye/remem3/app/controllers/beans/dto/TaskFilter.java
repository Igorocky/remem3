package org.igye.remem3.app.controllers.beans.dto;

public interface TaskFilter {
    boolean match(TaskView task);

    default TaskFilter not() {
        return task -> !this.match(task);
    }

    default TaskFilter and(TaskFilter other) {
        return task -> this.match(task) && other.match(task);
    }

    default TaskFilter or(TaskFilter other) {
        return task -> this.match(task) || other.match(task);
    }
}
