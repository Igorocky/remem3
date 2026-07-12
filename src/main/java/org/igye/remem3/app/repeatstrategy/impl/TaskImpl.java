package org.igye.remem3.app.repeatstrategy.impl;

import lombok.Getter;
import org.igye.remem3.app.controllers.exercise.HasBaseTask;
import org.igye.remem3.app.dto.RepeatStrategyType;
import org.igye.remem3.app.repeatstrategy.Hist;
import org.igye.remem3.app.repeatstrategy.HistRec;
import org.igye.remem3.app.repeatstrategy.Task;
import org.igye.remem3.utils.Exn;

import java.io.File;
import java.time.Instant;
import java.util.List;

public class TaskImpl implements Task, HasBaseTask {
    @Getter
    private final org.igye.remem3.app.dto.Task baseTask;
    private final Instant historyStartsAt;
    private final RepeatStrategyType repeatStrategyType;

    private int allHistSize;
    private Hist hist;
    private File file;
    private String dir;

    public TaskImpl(
        org.igye.remem3.app.dto.Task baseTask,
        Instant historyStartsAt,
        RepeatStrategyType repeatStrategyType
    ) {
        this.baseTask = baseTask;
        this.historyStartsAt = historyStartsAt;
        this.repeatStrategyType = repeatStrategyType;
    }

    @Override
    public Hist getHist() {
        List<org.igye.remem3.app.dto.HistRec> allHist = baseTask.getCard().getHistory();
        if (hist == null || allHistSize != allHist.size()) {
            allHistSize = allHist.size();
            hist = new HistImpl(
                allHist.stream()
                    .filter(histRec ->
                        histRec.getTime().compareTo(historyStartsAt) >= 0
                            && histRec.getStrategy() == repeatStrategyType
                            && baseTask.getTaskType().getCode().equals(histRec.getTaskType())
                    )
                    .map(HistRec.class::cast)
                    .toList()
            );
        }
        return hist;
    }

    @Override
    public String getDir() {
        if (dir == null) {
            dir = getFile().getParentFile().getAbsolutePath();
        }
        return dir;
    }

    @Override
    public RepeatStrategyType getSelectedByStrategyType() {
        return repeatStrategyType;
    }

    private File getFile() {
        if (file == null) {
            file = baseTask.getCard().getFile().orElseThrow(() -> new Exn("A file is not set for a card."));
        }
        return file;
    }
}
