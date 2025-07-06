package org.igye.remem3.app.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.Optional;

public sealed interface Task permits Task.FillGaps {
    Card getCard();

    Optional<HistRec> getLastHistRec();

    @Getter
    @Builder
    final class FillGaps implements Task {
        private Card card;
        private Optional<HistRec> lastHistRec;
    }

}
