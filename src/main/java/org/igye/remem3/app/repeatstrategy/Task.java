package org.igye.remem3.app.repeatstrategy;

import org.igye.remem3.app.dto.RepeatStrategyType;

import java.util.List;

public interface Task {
    List<HistRec> getHist();

    String getDir();

    RepeatStrategyType getSelectedByStrategyType();
}
