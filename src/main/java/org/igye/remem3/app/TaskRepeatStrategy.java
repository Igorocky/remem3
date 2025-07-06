package org.igye.remem3.app;

import org.igye.remem3.app.dto.Task;

public interface TaskRepeatStrategy {
    Task getNextTask();
}
