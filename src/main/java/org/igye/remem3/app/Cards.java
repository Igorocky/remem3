package org.igye.remem3.app;

import org.igye.remem3.app.dto.Card;
import org.igye.remem3.app.dto.HistRec;

import java.io.File;

public interface Cards {
    Card loadCard(File file);

    void saveCard(File file, Card card);

    void appendHistRecToFile(File file, HistRec histRec);
}
