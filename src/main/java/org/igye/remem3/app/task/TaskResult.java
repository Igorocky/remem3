package org.igye.remem3.app.task;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.igye.remem3.app.dto.HistRec;

public sealed interface TaskResult {

    @Getter
    @EqualsAndHashCode
    @ToString
    final class SaveHistRec implements TaskResult {
        private final HistRec histRec;

        public SaveHistRec(HistRec histRec) {
            this.histRec = histRec;
        }
    }

    @Getter
    @EqualsAndHashCode
    @ToString
    final class Completed implements TaskResult {
    }
}
