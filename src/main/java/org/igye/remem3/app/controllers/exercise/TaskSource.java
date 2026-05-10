package org.igye.remem3.app.controllers.exercise;

import org.igye.remem3.app.dto.Task;

import java.io.File;
import java.util.List;
import java.util.function.Function;

public interface TaskSource {
    List<File> getDirs();

    Function<Task, Boolean> getTaskFilter();
}
