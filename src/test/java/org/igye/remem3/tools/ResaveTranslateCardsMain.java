package org.igye.remem3.tools;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.igye.remem3.app.CardUtils;
import org.igye.remem3.app.dto.Card;
import org.igye.remem3.app.impl.CardUtilsImpl;
import org.igye.remem3.utils.impl.UtilsImpl;
import tools.jackson.databind.ObjectMapper;

import java.io.File;

@RequiredArgsConstructor
public class ResaveTranslateCardsMain {
    private final CardUtils cardUtils;

    static void main() {
        new ResaveTranslateCardsMain(new CardUtilsImpl(new UtilsImpl(new ObjectMapper()), null)).run();
    }

    @SneakyThrows
    private void run() {
        String rootDirStr = System.getenv("rootDir");
        File rootDir = new File(rootDirStr);

        cardUtils.loadAllCards(rootDir).stream()
            .filter(Card.Translate.class::isInstance)
            .forEach(card -> cardUtils.saveCard(card.getFile().get(), card));
    }
}
