package org.igye.remem3.app.controllers.beans.dto;

import org.igye.remem3.app.dto.TaskType;

import java.io.File;
import java.time.Instant;
import java.util.Optional;

public interface TaskView {
    TaskType getType();

    File getFile();

    int getPriority();

    Optional<Instant> getCreatedAt();
}
