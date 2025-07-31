package org.igye.remem3.controllers.components;

import org.apache.commons.lang3.tuple.Pair;
import org.igye.remem3.app.RepeatStrategyType;
import org.igye.remem3.app.dto.Task;
import org.igye.remem3.app.repeatstrategy.RepeatStrategy;
import org.igye.remem3.html.HtmlElem;

import java.util.List;

public interface RepeatStrategyCmp {

    RepeatStrategyType getStrategyType();

    HtmlElem render();

    RepeatStrategy makeRepeatStrategy(List<Task> tasks);

    List<Pair<String, String>> getProperties();

    void cacheState();
}
