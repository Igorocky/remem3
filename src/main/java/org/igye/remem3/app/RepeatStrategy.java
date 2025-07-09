package org.igye.remem3.app;

import org.igye.remem3.app.dto.Task;
import org.igye.remem3.html.HtmlElem;

import java.util.List;
import java.util.Optional;

public interface RepeatStrategy {
    Optional<List<Task>> getNextTasks();

    HtmlElem renderStats();
}
