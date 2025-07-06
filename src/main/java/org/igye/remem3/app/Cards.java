package org.igye.remem3.app;

import org.igye.remem3.app.dto.Card;
import org.igye.remem3.app.dto.HistRec;
import org.igye.remem3.controllers.newcard.CardDto;

import java.io.File;
import java.util.List;

public interface Cards {
    Card loadCard(File file);

    void saveCard(File file, Card card);

    List<String> validateCard(Card card);

    void appendHistRecToFile(File file, HistRec histRec);

    Card makeCard(CardDto cardDto);
}
