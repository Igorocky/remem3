package org.igye.remem3.app.controllers2.beans.dto;

import org.igye.remem3.app.dto.TaskType;

import java.io.File;
import java.time.Instant;
import java.util.Optional;

public interface TaskView {
    TaskType getTaskType();

    File getFile();

    Optional<Instant> getCreatedAt();
}
