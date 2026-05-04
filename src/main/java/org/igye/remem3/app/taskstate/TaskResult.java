package org.igye.remem3.app.taskstate;

import lombok.Data;
import org.igye.remem3.app.dto.HistRec;

import java.util.Optional;

@Data
public class TaskResult {
    private Optional<HistRec> histRec = Optional.empty();
    private boolean completed;
}
