package org.igye.remem3.tools;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.igye.remem3.app.CardUtils;
import org.igye.remem3.app.impl.CardUtilsImpl;
import org.igye.remem3.utils.impl.UtilsImpl;
import tools.jackson.databind.ObjectMapper;

import java.io.File;

@RequiredArgsConstructor
public class CorrectPriorityForCardsMain {
    private final CardUtils cardUtils;

    static void main() {
        new CorrectPriorityForCardsMain(new CardUtilsImpl(new UtilsImpl(new ObjectMapper()), null)).run();
    }

    @SneakyThrows
    private void run() {
        String rootDirStr = System.getenv("rootDir");
        File rootDir = new File(rootDirStr);

        cardUtils.loadAllCards(rootDir).stream()
            .filter(card -> card.getPriority() < 1 || card.getPriority() > 3)
            .forEach(card -> {
                card.setPriority(Math.clamp(card.getPriority(), 1, 3));
                cardUtils.saveCard(card);
            });
    }
}
