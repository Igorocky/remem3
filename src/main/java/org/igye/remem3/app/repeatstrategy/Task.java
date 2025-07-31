package org.igye.remem3.app.repeatstrategy;

import java.io.File;
import java.util.List;

public interface Task {
    List<HistRec> getHist();

    File getFile();

    String getId();

    String getDir();
}
