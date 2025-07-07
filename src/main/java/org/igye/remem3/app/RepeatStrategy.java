package org.igye.remem3.app;

import org.igye.remem3.app.dto.Task;

import java.util.Optional;

public interface RepeatStrategy {
    Optional<Task> getNextTask();
}
