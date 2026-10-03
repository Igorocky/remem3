package org.igye.remem3.app.controllers.beans.dto;

public interface TaskFilter {
    boolean match(TaskView task);

    default TaskFilter not() {
        TaskFilter self = this;
        return new TaskFilter() {
            @Override
            public boolean match(TaskView task) {
                return !self.match(task);
            }

            @Override
            public String toString() {
                return "NOT (%s)".formatted(self);
            }
        };
    }

    default TaskFilter and(TaskFilter other) {
        TaskFilter self = this;
        return new TaskFilter() {
            @Override
            public boolean match(TaskView task) {
                return self.match(task) && other.match(task);
            }

            @Override
            public String toString() {
                return "(%s AND %s)".formatted(self, other);
            }
        };
    }

    default TaskFilter or(TaskFilter other) {
        TaskFilter self = this;
        return new TaskFilter() {
            @Override
            public boolean match(TaskView task) {
                return self.match(task) || other.match(task);
            }

            @Override
            public String toString() {
                return "(%s OR %s)".formatted(self, other);
            }
        };
    }
}
