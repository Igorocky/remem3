package org.igye.remem3.app.dto;

import lombok.Getter;

public class Task {
    @Getter
    private final Card card;
    @Getter
    private final TaskType taskType;

    public Task(Card card, TaskType taskType) {
        this.card = card;
        this.taskType = taskType;
    }
}
