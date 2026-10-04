package org.igye.remem3.app.impl;

import org.igye.remem3.app.dto.Card;
import org.igye.remem3.app.imprt.CardCollectionDto;
import org.igye.remem3.app.imprt.ChapterDto;
import org.igye.remem3.app.imprt.QuestionAnswerCardDto;
import org.igye.remem3.app.imprt.TranslationCardDto;
import org.igye.remem3.utils.impl.UtilsImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.ObjectMapper;

import java.io.File;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

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
        List<String> errors = cards.importCards(baseDir, CardCollectionDto.builder()
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
            .build(), false);

        Assertions.assertEquals(List.of(), errors);
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
        Assertions.assertEquals(Map.of("generatedBy", "test"), qa.getAttrs());
        Card.Translate tr = rootCards.get(1);
        Assertions.assertEquals(new BigDecimal("2"), tr.getOrder());
        Assertions.assertEquals("LANG2", tr.getLang1());
        Assertions.assertEquals("text3", tr.getText1());
        Assertions.assertFalse(tr.isExactMatch1());
        Assertions.assertEquals("LANG1", tr.getLang2());
        Assertions.assertEquals("text4", tr.getText2());
        Assertions.assertFalse(tr.isExactMatch2());
        Assertions.assertEquals(Map.of("generatedBy", "test"), tr.getAttrs());

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

        Assertions.assertEquals(List.of(), cards.importCards(baseDir, collection, false));
        Assertions.assertEquals(List.of(), cards.importCards(baseDir, collection, false));

        List<Card.Translate> rootCards = loadCards(new File(baseDir, "root"));
        Assertions.assertEquals(2, rootCards.size());
        Assertions.assertEquals(new BigDecimal("2"), rootCards.get(1).getOrder());
        Assertions.assertEquals(Map.of(), rootCards.get(1).getAttrs());
    }

    @Test
    void importCards_puts_content_of_root_chapter_to_base_dir_when_root_dir_is_skipped() {
        CardCollectionDto collection = CardCollectionDto.builder()
            .cards(ChapterDto.builder()
                .chapterName("???")
                .chapters(List.of(
                    ChapterDto.builder()
                        .chapterName("child")
                        .cards(List.of(translation("LANG1", "text1", "LANG2", "text2")))
                        .build()
                ))
                .cards(List.of(translation("LANG1", "text3", "LANG2", "text4")))
                .build())
            .build();

        Assertions.assertEquals(List.of(), cards.importCards(baseDir, collection, true));

        Assertions.assertEquals(2, baseDir.list().length);
        List<Card.Translate> rootCards = loadCards(baseDir);
        Assertions.assertEquals(1, rootCards.size());
        Assertions.assertEquals("text3", rootCards.get(0).getText1());
        List<Card.Translate> childCards = loadCards(new File(baseDir, "child"));
        Assertions.assertEquals(1, childCards.size());
        Assertions.assertEquals("text1", childCards.get(0).getText1());
    }

    @Test
    void validateCardsForImport_returns_errors_and_writes_nothing() {
        CardCollectionDto validCollection = CardCollectionDto.builder()
            .cards(ChapterDto.builder()
                .chapterName("root")
                .cards(List.of(translation("LANG1", "text1", "LANG2", "text2")))
                .build())
            .build();
        CardCollectionDto invalidCollection = CardCollectionDto.builder()
            .cards(ChapterDto.builder()
                .chapterName("root")
                .cards(List.of(translation("LANG1", "text1", "LANG3", "text2")))
                .build())
            .build();

        Assertions.assertEquals(List.of(), cards.validateCardsForImport(baseDir, validCollection, false));
        Assertions.assertEquals(
            List.of("Chapter /root, card #1: Language2 'LANG3' is not registered."),
            cards.validateCardsForImport(baseDir, invalidCollection, false)
        );
        Assertions.assertEquals(
            List.of("The card collection doesn't have the root chapter."),
            cards.validateCardsForImport(baseDir, CardCollectionDto.builder().build(), false)
        );
        Assertions.assertEquals(0, baseDir.list().length);
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

        List<String> errors = cards.importCards(baseDir, collection, false);

        Assertions.assertEquals(
            List.of(
                "Chapter /root, card #2: Text1 is not set.",
                "Chapter /root/child, card #1: Language2 'LANG3' is not registered.",
                "Chapter /root/???: Directory name cannot be blank."
            ),
            errors
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

        List<String> errors = cards.importCards(baseDir, collection, false);

        Assertions.assertEquals(1, errors.size());
        Assertions.assertTrue(errors.get(0).startsWith("Chapter /root, card #1: The question language"));
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
