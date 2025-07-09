package org.igye.remem3.app.dto;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.igye.remem3.utils.Exn;

public sealed interface Task {
    Card getCard();

    TaskType getTaskType();

    String getId();

    @EqualsAndHashCode
    @ToString
    final class FillGaps implements Task {
        @Getter
        private final Card.FillGaps card;
        private TaskType taskType;
        private String id;

        public FillGaps(Card.FillGaps card) {
            this.card = card;
        }

        @Override
        public TaskType getTaskType() {
            if (taskType == null) {
                taskType = new TaskType.FillGaps(card.getLang());
            }
            return taskType;
        }

        @Override
        public String getId() {
            if (id == null) {
                id = card.getFile().orElseThrow(() -> new Exn("A file is not set for a card.")).getAbsolutePath()
                    + ":::" + taskType.getCode();
            }
            return id;
        }
    }

}
