package org.igye.remem3.app.dto;

import lombok.Getter;
import org.igye.remem3.utils.Exn;

public class Task {
    @Getter
    private final Card card;
    @Getter
    private final TaskType taskType;
    
    private String id;

    public Task(Card card, TaskType taskType) {
        this.card = card;
        this.taskType = taskType;
    }

    public String getId() {
        if (id == null) {
            id = card.getFile().orElseThrow(() -> new Exn("A file is not set for a card.")).getAbsolutePath()
                + ":::" + taskType.getCode();
        }
        return id;
    }
}
