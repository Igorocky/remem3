package org.igye.remem3.tools;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.igye.remem3.app.CardUtils;
import org.igye.remem3.app.dto.Card;
import org.igye.remem3.app.impl.CardUtilsImpl;
import org.igye.remem3.utils.impl.UtilsImpl;
import tools.jackson.databind.ObjectMapper;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@RequiredArgsConstructor
public class SetOrderForCardsMain {
    private final CardUtils cardUtils;

    static void main() {
        new SetOrderForCardsMain(new CardUtilsImpl(new UtilsImpl(new ObjectMapper()), null)).run();
    }

    @SneakyThrows
    private void run() {
        String rootDirStr = System.getenv("rootDir");
        File rootDir = new File(rootDirStr);
        Files.walk(rootDir.toPath())
            .map(Path::toFile)
            .filter(File::isDirectory)
            .forEach(this::setOrderForCardsInDir);
    }

    @SneakyThrows
    private void setOrderForCardsInDir(File dir) {
        List<Card> cards = cardUtils.loadCardsNonRec(dir).stream()
            .sorted(Comparator.comparing(card -> card.getCreatedAt().orElse(Instant.MIN)))
            .toList();
        for (int i = 0; i < cards.size(); i++) {
            Card card = cards.get(i);
            card.setOrder(new BigDecimal(i + 1));
            cardUtils.saveCard(card.getFile().get(), card);
        }
    }
}
