package org.igye.remem3.app.dto;

import lombok.Getter;
import org.igye.remem3.utils.Exn;

import java.io.File;

public class Task {
    @Getter
    private final Card card;
    @Getter
    private final TaskType taskType;

    private File file;
    private String id;
    private String dir;

    public Task(Card card, TaskType taskType) {
        this.card = card;
        this.taskType = taskType;
    }

    public File getFile() {
        if (file == null) {
            file = card.getFile().orElseThrow(() -> new Exn("A file is not set for a card."));
        }
        return file;
    }

    public String getId() {
        if (id == null) {
            id = getFile().getAbsolutePath() + ":::" + taskType.getCode();
        }
        return id;
    }

    public String getDir() {
        if (dir == null) {
            dir = getFile().getParentFile().getAbsolutePath();
        }
        return dir;
    }
}
