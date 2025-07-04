package org.igye.remem3.app.dto;

import java.io.File;
import java.util.List;

public interface Card {
    File getFile();

    List<HistRec> getHistory();
}
