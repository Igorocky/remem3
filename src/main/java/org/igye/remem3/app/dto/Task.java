package org.igye.remem3.app.dto;

import java.util.Optional;

public sealed interface Task permits TaskFillGaps {
    Card getCard();

    Optional<HistRec> getLastHistRec();
}
