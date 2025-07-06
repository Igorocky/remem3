package org.igye.remem3.app.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Optional;

@Getter
@Builder
public final class TaskFillGaps implements Task {
    private Card card;
    @Setter
    private Optional<HistRec> lastHistRec;
}
