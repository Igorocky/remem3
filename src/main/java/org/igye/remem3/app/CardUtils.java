package org.igye.remem3.app;

import org.igye.remem3.app.controllers.newcard.CardDto;
import org.igye.remem3.app.dto.Card;
import org.igye.remem3.app.dto.HistRec;
import org.igye.remem3.app.dto.fillgaps.TextPart;

import java.io.File;
import java.util.List;

public interface CardUtils {
    Card loadCard(File file);

    List<Card> loadAllCards(File dir);

    List<Card> loadCardsNonRec(File dir);

    void saveCard(File file, Card card);

    void saveCard(Card card);

    List<String> validateCard(Card card);

    List<String> getReferencedCards(Card card);

    void appendHistRecToFile(File file, HistRec histRec);

    Card makeCard(CardDto cardDto);

    String makeFileNameForCard(Card card);

    List<TextPart> parseText(String str);
}
