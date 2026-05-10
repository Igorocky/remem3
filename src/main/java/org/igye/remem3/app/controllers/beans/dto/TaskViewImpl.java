package org.igye.remem3.app.controllers.beans.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.igye.remem3.app.dto.Task;
import org.igye.remem3.app.dto.TaskType;

import java.io.File;
import java.time.Instant;
import java.util.Optional;

@AllArgsConstructor
@Builder
@Getter
public class TaskViewImpl implements TaskView {
    private final Task task;
    private final TaskType type;
    private final File file;
    private final Optional<Instant> createdAt;
}
