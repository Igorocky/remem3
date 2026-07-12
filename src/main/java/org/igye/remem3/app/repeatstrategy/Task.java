package org.igye.remem3.app.repeatstrategy;

import org.igye.remem3.app.dto.RepeatStrategyType;

public interface Task {
    Hist getHist();

    String getDir();

    RepeatStrategyType getSelectedByStrategyType();
}
