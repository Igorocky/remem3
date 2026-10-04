package org.igye.remem3.app.impl;

import org.igye.remem3.app.dto.Card;
import org.igye.remem3.app.imprt.CardCollectionDto;
import org.igye.remem3.app.imprt.ChapterDto;
import org.igye.remem3.app.imprt.QuestionAnswerCardDto;
import org.igye.remem3.app.imprt.TranslationCardDto;
import org.igye.remem3.utils.Exn;
import org.igye.remem3.utils.impl.UtilsImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.ObjectMapper;

import java.io.File;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

class CardUtilsImplImportCardsTest {
    @TempDir
    File baseDir;

    private final CardUtilsImpl cards = new CardUtilsImpl(
        new UtilsImpl(new ObjectMapper()),
        SettingsImpl.builder()
            .languages(List.of("LANG1", "LANG2", "QLANG", "ALANG"))
            .questionLanguage("QLANG")
            .answerLanguage("ALANG")
            .build()
    );

    @Test
    void importCards_creates_directory_per_chapter_and_saves_cards() {
        cards.importCards(baseDir, CardCollectionDto.builder()
            .generatedBy("test")
            .cards(ChapterDto.builder()
                .chapterName("Root: chapter")
                .chapters(List.of(
                    ChapterDto.builder()
                        .chapterName("child 1")
                        .cards(List.of(translation("LANG1", "text1", "LANG2", "text2")))
                        .build(),
                    ChapterDto.builder().chapterName("child 2").build()
                ))
                .cards(List.of(
                    QuestionAnswerCardDto.builder().question("question1").answer("answer1").build(),
                    translation("LANG2", "text3", "LANG1", "text4")
                ))
                .build())
            .build());

        File rootDir = new File(baseDir, "Root chapter");
        Assertions.assertTrue(rootDir.isDirectory());
        Assertions.assertTrue(new File(rootDir, "child 2").isDirectory());

        List<Card.Translate> rootCards = loadCards(rootDir);
        Assertions.assertEquals(2, rootCards.size());
        Card.Translate qa = rootCards.get(0);
        Assertions.assertEquals(new BigDecimal("1"), qa.getOrder());
        Assertions.assertEquals("QLANG", qa.getLang1());
        Assertions.assertEquals("question1", qa.getText1());
        Assertions.assertFalse(qa.isExactMatch1());
        Assertions.assertEquals("ALANG", qa.getLang2());
        Assertions.assertEquals("answer1", qa.getText2());
        Assertions.assertFalse(qa.isExactMatch2());
        Assertions.assertTrue(qa.getCreatedAt().isPresent());
        Card.Translate tr = rootCards.get(1);
        Assertions.assertEquals(new BigDecimal("2"), tr.getOrder());
        Assertions.assertEquals("LANG2", tr.getLang1());
        Assertions.assertEquals("text3", tr.getText1());
        Assertions.assertFalse(tr.isExactMatch1());
        Assertions.assertEquals("LANG1", tr.getLang2());
        Assertions.assertEquals("text4", tr.getText2());
        Assertions.assertFalse(tr.isExactMatch2());

        List<Card.Translate> childCards = loadCards(new File(rootDir, "child 1"));
        Assertions.assertEquals(1, childCards.size());
        Assertions.assertEquals("text1", childCards.get(0).getText1());
        Assertions.assertEquals("text2", childCards.get(0).getText2());
    }

    @Test
    void importCards_reuses_existing_directories() {
        CardCollectionDto collection = CardCollectionDto.builder()
            .cards(ChapterDto.builder()
                .chapterName("root")
                .cards(List.of(translation("LANG1", "text1", "LANG2", "text2")))
                .build())
            .build();

        cards.importCards(baseDir, collection);
        cards.importCards(baseDir, collection);

        List<Card.Translate> rootCards = loadCards(new File(baseDir, "root"));
        Assertions.assertEquals(2, rootCards.size());
        Assertions.assertEquals(new BigDecimal("2"), rootCards.get(1).getOrder());
    }

    @Test
    void importCards_writes_nothing_when_there_are_errors() {
        CardCollectionDto collection = CardCollectionDto.builder()
            .cards(ChapterDto.builder()
                .chapterName("root")
                .chapters(List.of(
                    ChapterDto.builder()
                        .chapterName("child")
                        .cards(List.of(translation("LANG1", "text1", "LANG3", "text2")))
                        .build(),
                    ChapterDto.builder().chapterName("???").build()
                ))
                .cards(List.of(
                    translation("LANG1", "text3", "LANG2", "text4"),
                    translation("LANG1", " ", "LANG2", "text5")
                ))
                .build())
            .build();

        Exn ex = Assertions.assertThrows(Exn.class, () -> cards.importCards(baseDir, collection));

        Assertions.assertEquals(
            """
                Cannot import cards:
                Chapter /root, card #2: Text1 is not set.;
                Chapter /root/child, card #1: Language2 'LANG3' is not registered.;
                Chapter /root/???: Directory name cannot be blank.""",
            ex.getMessage()
        );
        Assertions.assertEquals(0, baseDir.list().length);
    }

    @Test
    void importCards_fails_for_question_answer_card_when_languages_are_not_set() {
        CardUtilsImpl cards = new CardUtilsImpl(
            new UtilsImpl(new ObjectMapper()), SettingsImpl.builder().languages(List.of("LANG1", "LANG2")).build()
        );
        CardCollectionDto collection = CardCollectionDto.builder()
            .cards(ChapterDto.builder()
                .chapterName("root")
                .cards(List.of(QuestionAnswerCardDto.builder().question("question1").answer("answer1").build()))
                .build())
            .build();

        Exn ex = Assertions.assertThrows(Exn.class, () -> cards.importCards(baseDir, collection));

        Assertions.assertTrue(ex.getMessage().contains("Chapter /root, card #1: The question language"));
        Assertions.assertEquals(0, baseDir.list().length);
    }

    private TranslationCardDto translation(String fromLang, String text, String toLang, String translatedText) {
        return TranslationCardDto.builder()
            .fromLanguage(fromLang).textToTranslate(text)
            .toLanguage(toLang).translatedText(translatedText)
            .build();
    }

    private List<Card.Translate> loadCards(File dir) {
        return cards.loadCardsNonRec(dir).stream()
            .map(card -> (Card.Translate) card)
            .sorted(Comparator.comparing(Card::getOrder))
            .toList();
    }
}
