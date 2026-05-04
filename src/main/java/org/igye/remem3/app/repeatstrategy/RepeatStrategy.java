package org.igye.remem3.app.repeatstrategy;

import org.apache.commons.lang3.tuple.Pair;
import org.igye.remem3.html.HtmlElem;

import java.util.List;
import java.util.Optional;

public interface RepeatStrategy {
    Optional<List<Task>> getNextTasks();

    HtmlElem renderLessParams(boolean historyUpdated);

    HtmlElem renderMoreParams(boolean historyUpdated);

    boolean hasDailyUniqueCount();

    Optional<Pair<Long, Long>> getDailyUniqueCount();
}
